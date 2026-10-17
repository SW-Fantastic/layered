package org.swdc.clang.libtooling.generator;

import java.util.HashMap;
import java.util.Map;

public class CXXMetadata {

    private Map<String, WritableFunction> symbols = new HashMap<>();

    private Map<String, WritableFunction> callbacks = new HashMap<>();

    public Map<String, WritableFunction> getSymbols() {
        return symbols;
    }

    public void setSymbols(Map<String, WritableFunction> symbols) {
        this.symbols = symbols;
    }

    public void addFunction(WritableFunction function) {
        symbols.put(function.getMangledName(), function);
    }

    public void addCallback(WritableFunction callback) {
        callbacks.put(callback.getMangledName(), callback);
    }

}
