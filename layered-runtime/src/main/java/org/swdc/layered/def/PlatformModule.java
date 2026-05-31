package org.swdc.layered.def;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.swdc.layered.ExternalInvoker;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.pointers.OpaquePointer;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlatformModule extends OpaquePointer {

    private static ConcurrentHashMap<String, PlatformModule> loadedModules = new ConcurrentHashMap<>();

    private File library;

    private Map<String, WritableFunction> symbolTable = null;

    private PlatformCallHandler callHandler = null;

    private PlatformModule(Allocator allocator, Long address, File library, ByteBuffer metaData) {

        this.library = library;
        this.initPointer(allocator,address,false);
        if (metaData != null) {
            try {
                ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());
                JavaType symbolTableType = mapper.getTypeFactory()
                        .constructParametricType(HashMap.class, String.class, WritableFunction.class);
                byte[] bytes = new byte[metaData.capacity()];
                metaData.get(bytes);
                symbolTable = mapper.readValue(bytes, symbolTableType);
                callHandler = new PlatformCallHandler(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public File getLibrary() {
        return library;
    }

    public Map<String, WritableFunction> getSymbolTable() {
        return symbolTable;
    }

    public <T> T createCallProxy(Class<T> nativeInterface) {
        return (T)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{nativeInterface}, callHandler);
    }

    @Override
    protected void deAllocate() {
        if (isNull()) {
            return;
        }
        if (callHandler != null) {
            callHandler.free();
            callHandler = null;
        }
        ExternalInvoker.unloadLibrary(getAddress());
    }

    public static PlatformModule load(Allocator allocator, File library) {

        if (lookup(library) != null) {
            return loadedModules.get(library.getAbsolutePath());
        }

        long address = ExternalInvoker.loadLibrary(library.getAbsolutePath());
        if (address == 0) {
            String reason = ExternalInvoker.getLastError();
            throw new IllegalStateException(reason);
        }
        ByteBuffer metadata = ExternalInvoker.getLibrarySymbols(address);
        if (metadata == null) {
            String reason = ExternalInvoker.getLastError();
            throw new IllegalStateException(reason);
        }
        PlatformModule module = new PlatformModule(allocator,address,library,metadata);
        loadedModules.put(library.getAbsolutePath(), module);
        return module;

    }

    public static PlatformModule lookup(File library) {
        if (loadedModules.containsKey(library.getAbsolutePath())) {
            return loadedModules.get(library.getAbsolutePath());
        }
        return null;
    }

    public long lookup(String symbol) {

        if (symbolTable == null) {
            throw new IllegalStateException("can not find symbol table");
        }
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalStateException("symbol is null or empty");
        }
        return ExternalInvoker.lookup(getAddress(),symbol);

    }

}
