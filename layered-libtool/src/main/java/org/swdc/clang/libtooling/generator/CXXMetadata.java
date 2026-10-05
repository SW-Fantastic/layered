package org.swdc.clang.libtooling.generator;

import java.util.HashMap;
import java.util.Map;

public class CXXMetadata {

    private String prefix;

    private Map<String, WritableFunction> symbols = new HashMap<>();


    public String getPrefix() {
        return prefix;
    }

    public Map<String, WritableFunction> getSymbols() {
        return symbols;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setSymbols(Map<String, WritableFunction> symbols) {
        this.symbols = symbols;
    }

    public void addFunction(WritableFunction function) {
        symbols.put(function.getMangledName(), function);
    }

}
