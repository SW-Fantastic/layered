package org.swdc.layered.calling;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.swdc.layered.ExternalInvoker;
import org.swdc.layered.LayerMetadata;
import org.swdc.layered.library.LayerLibrary;
import org.swdc.layered.library.LayerRuntimeLibrary;
import org.swdc.layered.library.LibraryResource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;

public class LayerModule {

    private LayerLibrary library;

    private Map<File, Long> loaded = new ConcurrentHashMap<>();

    /**
     * 本地库名称 - 地址映射表，用于后续调用时快速定位库的加载地址。
     */
    private Map<String, Long> loadedHandles = new ConcurrentHashMap<>();

    /**
     * 本地库名称 - 符号数据映射表，用于快速定位库的符号信息。
     */
    private Map<String, LayerMetadata> moduleSymbols = new ConcurrentHashMap<>();

    /**
     * 缓存的调用代理，用于快速定位已创建的调用代理。
     */
    private Map<Class, Object> callingProxies = new ConcurrentHashMap<>();

    /**
     * 缓存的调用Invoker，用于快速定位已创建的调用Invoker。
     */
    private Map<String, LayerInvoker> callingInvokers = new ConcurrentHashMap<>();
    
    private ReentrantLock lock = new ReentrantLock();


    public LayerModule(LayerLibrary library) {
        this.library = library;
    }

    /**
     * 从指定资源目录解析并加载本模块依赖的原生动态库。
     * <p>
     * 该方法首先解析资源目录中的库描述信息，然后遍历解析出的每个库描述，
     * 加载对应的原生动态库文件，并将库文件及其加载地址缓存到内部映射中，
     * 供后续调用与卸载时使用。
     *
     * @param resourceFolder 包含动态库及其描述信息的资源目录
     * @throws RuntimeException 当库文件不存在或加载失败时抛出
     */
    public void load(File resourceFolder) {

        try {
            lock.lock();

            StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
            Class caller = stackWalker.getCallerClass();

            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            LayerRuntimeLibrary runtimeLibrary = LayerRuntimeLibrary.getInstance();
            runtimeLibrary.load(resourceFolder);

            library.resolve(caller, resourceFolder, true);
            for (LibraryResource library: library.getDescriptors()) {
                File file = this.library.getResolvedLibrary(library.getName());
                if (file == null || !file.exists()) {
                    throw new RuntimeException("Library " + library.getName() + " does not exist");
                }
                long libraryAddr = ExternalInvoker.loadLibrary(file.getAbsolutePath());
                if (libraryAddr == 0) {
                    throw new RuntimeException("Failed to load library " + library.getName());
                }

                loaded.put(file, libraryAddr);
                loadedHandles.put(library.getName(), libraryAddr);
            }
            
        } finally {
            lock.unlock();
        }
        

    }


    /**
     * 获取指定模块的API代理对象。
     * <p>
     * 该方法根据传入的模块名称和接口类型，返回一个动态代理对象。
     * 代理对象会拦截所有方法调用，并通过LayerInvoker转发到原生库。
     * 若该接口类型已存在对应的代理，则直接返回缓存的代理实例。
     *
     * @param <T>       泛型类型，表示要获取的API接口类型
     * @param moduleName 模块名称，用于定位原生库和对应的元数据
     * @param clazz     要获取的API接口Class对象
     * @return 实现了指定接口的动态代理对象
     * @throws RuntimeException 当模块未加载或元数据不存在时抛出
     */
    public <T> T getAPI(String moduleName, Class<T> clazz) {

        try {
            lock.lock();

            if (callingProxies.containsKey(clazz)) {
                return (T)callingProxies.get(clazz);
            }

            LayerMetadata metadata = loadModuleMetadata(moduleName);
            if (metadata == null) {
                throw new RuntimeException("Module " + moduleName + " is not loaded");
            }
            Long libraryAddr = loadedHandles.get(moduleName);
            LayerInvoker invoker = new LayerInvoker(libraryAddr, metadata);
            Object proxy = Proxy.newProxyInstance(
                    clazz.getClassLoader(),
                    new Class[]{clazz},
                    new LayerInvokeHandler(invoker)
            );
            callingInvokers.put(moduleName, invoker);
            callingProxies.put(clazz, proxy);
            return (T)proxy;
            
        } finally {
            lock.unlock();
        }

    }



    /**
     * 加载指定名称的模块元数据。
     * <p>
     * 首先从缓存中查找已加载的模块，若未命中则从已加载的原生库中读取符号表并解析为元数据。
     *
     * @param moduleName 模块名称
     * @return 模块的元数据对象
     * @throws RuntimeException 当模块未加载或元数据解析失败时抛出
     */
    public LayerMetadata loadModuleMetadata(String moduleName) {

        try {
            
            lock.lock();
            if (moduleSymbols.containsKey(moduleName)) {
                return moduleSymbols.get(moduleName);
            }

            if (!loadedHandles.containsKey(moduleName)) {
                throw new RuntimeException("Module " + moduleName + " is not loaded");
            }

            Long libraryAddr = loadedHandles.get(moduleName);
            ByteBuffer symbols = ExternalInvoker.getLibrarySymbols(libraryAddr);
            if (symbols == null) {
                throw new RuntimeException(moduleName + " is not a layer module");
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            try {
                symbols.rewind();
                byte[] bytes = new byte[symbols.remaining()];
                symbols.get(bytes);

                GZIPInputStream gin = new GZIPInputStream(new ByteArrayInputStream(bytes));
                LayerMetadata metadata = mapper.readValue(gin, LayerMetadata.class);
                moduleSymbols.put(moduleName, metadata);
                return metadata;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
        } finally {
            lock.unlock();
        }

    }


    public void unload() {

        try {
            lock.lock();
            for (LayerInvoker invoker: callingInvokers.values()) {
                invoker.close();
            }

            callingProxies.clear();
            callingInvokers.clear();
            moduleSymbols.clear();
            loadedHandles.clear();
            for (LibraryResource library: library.getDescriptors()) {
                File file = this.library.getResolvedLibrary(library.getName());
                Long libraryAddr = loaded.get(file);
                if (libraryAddr != null) {
                    ExternalInvoker.unloadLibrary(libraryAddr);
                }
            }
        } finally {
            lock.unlock();
        }

    }


}
