package org.swdc.layered.calling;

import org.swdc.layered.ExternalInvoker;
import org.swdc.layered.LayerFunction;
import org.swdc.layered.LayerMetadata;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.types.Symbol;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class LayerInvoker {

    private volatile Long moduleAddress;
    private LayerMetadata metadata;
    private Allocator allocator;

    private Map<Method, Long> methodAddressMap = new ConcurrentHashMap<>();
    private Map<Method, LayerCIFHandler> handlerMap = new ConcurrentHashMap<>();

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
        try {
            lock.readLock().lock();

            if (handlerMap.containsKey(method)) {
                return handlerMap.get(method);
            }
            LayerFunction function = lookupFunction(method);
            LayerCIFHandler cifHandler = new LayerCIFHandler(this, function);
            handlerMap.put(method, cifHandler);
            return cifHandler;

        } finally {
            lock.readLock().unlock();
        }
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
            String mangledName = generateMangledName(method);
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
        returnTypeSymbol = "(" +  returnTypeSymbol + ")";


        StringJoiner joiner = new StringJoiner(",");
        for (int index = 0; index < method.getParameterCount(); index++) {
            Parameter parameter = method.getParameters()[index];
            String paramTypeSymbol = TypeUtils.getTypeSymbol(parameter.getAnnotatedType(), parameter);
            joiner.add(paramTypeSymbol);
        }

        String paramList = "@" + joiner;
        Symbol symbol = method.getAnnotation(Symbol.class);
        if (symbol != null) {
            return returnTypeSymbol + symbol.value() + paramList;
        }
        return returnTypeSymbol + method.getName() + paramList;

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

    public void close() {
        try {
            lock.writeLock().lock();

            for (LayerCIFHandler handler : handlerMap.values()) {
                handler.close();
            }
            handlerMap.clear();

            this.metadata = null;
            this.moduleAddress = null;
            this.allocator.free();
            this.allocator = null;
        } finally {
            lock.writeLock().unlock();
        }

    }

}
