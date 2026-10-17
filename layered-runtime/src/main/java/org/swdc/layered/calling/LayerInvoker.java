package org.swdc.layered.calling;

import org.swdc.layered.ExternalInvoker;
import org.swdc.layered.LayerFunction;
import org.swdc.layered.LayerMetadata;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.types.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 本地函数调用器。
 * <p>此类封装了与本地库的交互，包括函数调用、回调管理等。</p>
 */
public class LayerInvoker {

    private volatile Long moduleAddress;
    private LayerMetadata metadata;
    private Allocator allocator;

    private Map<Method, Long> methodAddressMap = new ConcurrentHashMap<>();
    private Map<Method, String> methodSymbolMap = new ConcurrentHashMap<>();
    private Map<Method, LayerCIFHandler> handlerMap = new ConcurrentHashMap<>();

    private Map<Method, LayerClosureCIFHandler> callbackCIFAddressMap = new ConcurrentHashMap<>();
    private Map<Method, List<LayerClosure<?>>> activeCallbacks = new ConcurrentHashMap<>();

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public LayerInvoker(Long moduleAddress, LayerMetadata metadata) {
        this.moduleAddress = moduleAddress;
        this.metadata = metadata;
        this.allocator = new Allocator();
    }

    public LayerMetadata getMetadata() {
        return metadata;
    }

    public LayerCIFHandler getCIFHandler(Method method) {

        return handlerMap.computeIfAbsent(method, k -> {
            LayerFunction function = lookupFunction(k);
            return new LayerCIFHandler(this, function);
        });

    }
    
    /**
     * 根据 Method 对象查找对应的函数元数据。
     *
     * <p>该方法会先校验传入的方法对象，再根据方法签名生成修饰名称，
     * 并从元数据的符号表中查找对应的函数定义。</p>
     *
     * @param method 目标方法对象，不能为 null
     * @return 与该方法对应的函数元数据
     * @throws IllegalArgumentException 当 method 为 null 时抛出
     * @throws RuntimeException 当方法对应的修饰名称不存在于符号表中时抛出
     * @see #generateMangledName(Method)
     */
    public LayerFunction lookupFunction(Method method) {

        try {
            lock.readLock().lock();
            if (method == null) {
                throw new IllegalArgumentException("Cannot lookup null method");
            }

            String mangledName = methodSymbolMap.get(method);
            if (mangledName == null) {
                mangledName = generateMangledName(method);
            }
            if (metadata.getSymbols().containsKey(mangledName)) {
                return metadata.getSymbols().get(mangledName);
            }

            throw new RuntimeException("Cannot find method: " + mangledName);
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * 根据 Method 对象查找对应的本地方法地址。
     *
     * <p>该方法会首先检查本地缓存，若命中则直接返回；未命中时根据方法签名生成
     * 修饰名称，再委托 {@link #lookupMethodAddress(String)} 进行实际查找。</p>
     *
     * @param method 目标方法对象，不能为 null
     * @return 方法的本地内存地址
     * @throws RuntimeException 当方法对应的修饰名称不存在于符号表中或无法解析到地址时抛出
     * @see #lookupMethodAddress(String)
     */
    public Long lookupMethodAddress(Method method) {

        try {

            lock.writeLock().lock();

            if (methodAddressMap.containsKey(method)) {
                return methodAddressMap.get(method);
            }

            long addr = lookupMethodAddress(
                    generateMangledName(method)
            );

            methodAddressMap.put(method, addr);
            return addr;

        } finally {
            lock.writeLock().unlock();
        }

    }


    /**
     * 根据函数元数据对象查找对应的本地方法地址。
     *
     * <p>该方法会先校验传入的函数对象，再使用其目标名称（targetName）
     * 进行实际的方法地址查找。</p>
     *
     * @param function 目标函数元数据，不能为 null
     * @return 方法的本地内存地址
     * @throws IllegalArgumentException 当 function 为 null 时抛出
     * @see #lookupMethodAddress(String)
     */
    public Long lookupMethodAddress(LayerFunction function) {

        try {

            lock.readLock().lock();
            if (function == null ) {
                throw new RuntimeException("Cannot find symbol with null function");
            }

            long methodAddress = ExternalInvoker.lookup(
                    moduleAddress,
                    function.getTargetName()
            );
            if (methodAddress != 0) {
                return methodAddress;
            }
            throw new RuntimeException("Cannot find method: " + function.getMangledName());

        } finally {
            lock.readLock().unlock();
        }
    
    }
    
    /**
     * 根据修饰名称查找对应的本地方法地址。
     *
     * <p>该方法会先校验修饰名称是否为空且存在于元数据的符号表中，
     * 校验通过后从模块中解析该符号的实际内存地址。</p>
     *
     * @param mangledName 方法的修饰名称，不能为 null 且需存在于符号表中
     * @return 方法的本地内存地址
     * @throws RuntimeException 当修饰名称为空、不存在于符号表中或无法解析到地址时抛出
     */
    public Long lookupMethodAddress(String mangledName) {

        try {

            lock.readLock().lock();
            if (mangledName == null || mangledName.isBlank() || !metadata.getSymbols().containsKey(mangledName)) {
                throw new RuntimeException("Cannot find method: " + mangledName);
            }

            LayerFunction function = metadata.getSymbols().get(mangledName);
            return lookupMethodAddress(function);

        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * 根据回调方法的签名生成对应的修饰名称（mangled name）。
     *
     * <p>依次解析方法的返回值类型符号与各参数类型符号，按
     * {@code (返回类型符号)_callback_参数个数@参数类型符号1,参数类型符号2,...} 的格式拼接，
     * 用于在模块的回调表中查找匹配的回调函数。</p>
     *
     * @param method 回调接口中的方法
     * @return 该方法对应的修饰名称
     */
    private String generateCbMangledName(Method method) {

        String returnTypeSymbol = TypeUtils.getTypeSymbol(method.getAnnotatedReturnType(), method) ;

        StringJoiner joiner = new StringJoiner(",");
        for (int index = 0; index < method.getParameterCount(); index++) {
            Parameter parameter = method.getParameters()[index];
            String paramTypeSymbol = TypeUtils.getTypeSymbol(parameter.getAnnotatedType(), parameter);
            joiner.add(paramTypeSymbol);
        }

        return "(" + returnTypeSymbol + ")_callback_" + method.getParameterCount() + "@" + joiner;

    }

    /**
     * 为指定的回调接口创建闭包实例。
     *
     * <p>遍历接口 {@code clazz} 的所有方法，根据方法签名生成修饰名称，
     * 并在模块的回调表中查找匹配的 {@link LayerFunction}；找到后创建对应的
     * {@link LayerClosure} 并注册到活跃回调列表中。</p>
     *
     * @param clazz    回调接口类型，不能为 null
     * @param instance 回调接口的实现实例，不能为 null
     * @param <T>      回调接口类型
     * @return 创建的回调闭包实例
     * @throws RuntimeException 模块未定义回调，或未找到匹配的回调方法时抛出
     */
    public <T> LayerClosure<T> createClosure(Class<T> clazz, T instance) {

        try {
            lock.readLock().lock();
            LayerMetadata metadata = getMetadata();
            Map<String, LayerFunction> callbacks = metadata.getCallbacks();
            if(callbacks == null) {
                throw new RuntimeException("Module has no callbacks");
            }

            for (Method method : clazz.getMethods()) {
                String mangledName = generateCbMangledName(method);
                if (callbacks.containsKey(mangledName)) {
                    LayerFunction function = callbacks.get(mangledName);
                    LayerClosureCIFHandler cifHandler = callbackCIFAddressMap.computeIfAbsent(
                            method,
                            m -> new LayerClosureCIFHandler(function)
                    );
                    LayerClosure<T> closure = new LayerClosure<>(this,method,instance,cifHandler);
                    List<LayerClosure<?>> closures = this.activeCallbacks.computeIfAbsent(
                            method, m -> new ArrayList<>()
                    );
                    closures.add(closure);
                    return closure;
                }
            }

            throw new RuntimeException("Cannot find suitable callback method.");

        } finally {
            lock.readLock().unlock();
        }

    }


   
    /**
     * 根据方法签名生成符合本机调用规范的修饰名称。
     *
     * <p>修饰名称格式为：返回类型符号 + 符号名 + @ + 参数类型符号列表</p>
     *
     * <p>如果方法标注了 {@link Symbol} 注解，则使用注解值作为符号名；否则使用方法名。</p>
     *
     * @param method 目标方法，不能为 null
     * @return 生成的修饰名称字符串
     */
    public String generateMangledName(Method method) {

        String returnTypeSymbol = TypeUtils.getTypeSymbol(method.getAnnotatedReturnType(), method) ;

        StringJoiner joiner = new StringJoiner(",");
        for (int index = 0; index < method.getParameterCount(); index++) {
            Parameter parameter = method.getParameters()[index];
            String paramTypeSymbol = TypeUtils.getTypeSymbol(parameter.getAnnotatedType(), parameter);
            joiner.add(paramTypeSymbol);
        }

        String paramList = "@" + joiner;
        Symbol symbol = method.getAnnotation(Symbol.class);
        if (symbol != null) {
            return "(" + returnTypeSymbol + ")" + symbol.value() + paramList;
        }

        Constructor ctor = method.getDeclaredAnnotation(Constructor.class);
        if (ctor != null) {
            return "(" + returnTypeSymbol + ")" + TypeUtils.getClassSymbol(
                    ctor.value()
            ) + "_ctor" + paramList;
        }

        Destructor dtor = method.getDeclaredAnnotation(Destructor.class);
        if (dtor != null) {
            return "(" + returnTypeSymbol + ")" + TypeUtils.getClassSymbol(
                    dtor.value()
            ) + "_dtor" + paramList;
        }

        FieldGetter getter = method.getDeclaredAnnotation(FieldGetter.class);
        if (getter != null) {
            return "(" + returnTypeSymbol + ")" + TypeUtils.getClassSymbol(
                    getter.type()
            ) + "::get_" + getter.field() + paramList;
        }

        FieldSetter setter = method.getDeclaredAnnotation(FieldSetter.class);
        if (setter != null) {
            return "(" + returnTypeSymbol + ")" + TypeUtils.getClassSymbol(
                    setter.type()
            ) + "::set_" + setter.field() + paramList;
        }

        return "(" + returnTypeSymbol + ")"  + method.getName() + paramList;

    }

    public Allocator getAllocator() {
        try {
            lock.readLock().lock();
            if (allocator == null) {
                throw new IllegalStateException("Invoker has closed");
            }
            return allocator;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Long getModuleAddress() {
        try {
            lock.readLock().lock();
            if (moduleAddress == null) {
                throw new IllegalStateException("Invoker has closed");
            }
            return moduleAddress;
        } finally {
            lock.readLock().unlock();
        }

    }

    
    /**
     * 注销指定方法下已注册的回调闭包。
     *
     * <p>该方法在读锁保护下从活跃回调表中移除给定闭包，当该方法对应的闭包列表变为空时，
     * 一并移除其映射项，以避免残留无用的回调记录。</p>
     *
     * @param method 闭包所属的回调方法
     * @param closure 待注销的回调闭包
     */
    void closureUnref(Method method, LayerClosure<?> closure) {
        try {
            lock.readLock().lock();
            List<LayerClosure<?>> closures = activeCallbacks.get(method);
            if (closures != null) {
                closures.remove(closure);
                if (closures.isEmpty()) {
                    activeCallbacks.remove(method);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 关闭并释放该调用器持有的全部本地资源。
     *
     * <p>该方法在写锁保护下依次关闭所有活跃回调闭包、方法 CIF 处理器以及回调 CIF 处理器，
     * 清空对应的缓存映射与符号表，随后将元数据、模块地址置空，并释放分配器。
     * 执行完成后该调用器不可再用于方法查找或调用。</p>
     */
    public void close() {

        try {
            lock.writeLock().lock();

            for (List<LayerClosure<?>> closures : activeCallbacks.values()) {
                for (LayerClosure<?> closure : closures) {
                    closure.close();
                }
            }
            activeCallbacks.clear();

            for (LayerCIFHandler handler : handlerMap.values()) {
                handler.close();
            }
            handlerMap.clear();

            for (LayerClosureCIFHandler handler : callbackCIFAddressMap.values()) {
                handler.close();
            }
            callbackCIFAddressMap.clear();

            methodSymbolMap.clear();
            this.metadata = null;
            this.moduleAddress = null;
            this.allocator.free();
            this.allocator = null;
        } finally {
            lock.writeLock().unlock();
        }

    }

}
