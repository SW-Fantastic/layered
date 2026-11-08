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

        if(this.moduleAddress == null) {
            throw new IllegalStateException("Invoker has closed");
        }

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
     * @see TypeUtils#generateMangledName(Method)
     */
    public LayerFunction lookupFunction(Method method) {

        try {
            if(this.moduleAddress == null) {
                throw new IllegalStateException("Invoker has closed");
            }

            lock.readLock().lock();
            if (method == null) {
                throw new IllegalArgumentException("Cannot lookup null method");
            }

            String mangledName = methodSymbolMap.computeIfAbsent(method, k -> {
                Class<?> declaringClass = k.getDeclaringClass();
                Closure closure = declaringClass.getAnnotation(Closure.class);
                if (closure != null) {
                    return TypeUtils.generateCbMangledName(k);
                } else {
                    return TypeUtils.generateMangledName(k);
                }
            });

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

            if(this.moduleAddress == null) {
                throw new IllegalStateException("Invoker has closed");
            }

            lock.writeLock().lock();

            if (methodAddressMap.containsKey(method)) {
                return methodAddressMap.get(method);
            }

            String mangledName = null;
            Class<?> declaringClass = method.getDeclaringClass();
            Closure closure = declaringClass.getAnnotation(Closure.class);
            if (closure != null) {
                mangledName = TypeUtils.generateCbMangledName(method);
            } else {
                mangledName = TypeUtils.generateMangledName(method);
            }

            long addr = lookupMethodAddress(mangledName);

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

            if(this.moduleAddress == null) {
                throw new IllegalStateException("Invoker has closed");
            }

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

            if(this.moduleAddress == null) {
                throw new IllegalStateException("Invoker has closed");
            }

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

    public LayerClosureCIFHandler getClosureCIFHandler(Method method) {

        if(this.moduleAddress == null) {
            throw new IllegalStateException("Invoker has closed");
        }

        return callbackCIFAddressMap.computeIfAbsent(method, k -> {
            LayerFunction function = lookupFunction(k);
            return new LayerClosureCIFHandler(this, function);
        });

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

            Closure target = clazz.getDeclaredAnnotation(Closure.class);
            if (target == null) {
                throw new RuntimeException("No @Closure annotation found on " + clazz.getName());
            }

            if (target.value() == null || target.value().isBlank()) {
                throw new RuntimeException("No method name specified for " + clazz.getName());
            }

            return getAllocator().createClosure(
                    this, clazz, instance
            );

        } finally {
            lock.readLock().unlock();
        }

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
     * 关闭并释放该调用器持有的全部本地资源。
     *
     * <p>该方法在写锁保护下依次关闭所有活跃回调闭包、方法 CIF 处理器以及回调 CIF 处理器，
     * 清空对应的缓存映射与符号表，随后将元数据、模块地址置空，并释放分配器。
     * 执行完成后该调用器不可再用于方法查找或调用。</p>
     */
    public void close() {

        try {
            lock.writeLock().lock();

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
