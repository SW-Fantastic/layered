package org.swdc.layered;

import java.util.HashMap;
import java.util.Map;

public class LayerMetadata {

    private String prefix;

    private Map<String, LayerFunction> symbols = new HashMap<>();


    public String getPrefix() {
        return prefix;
    }

    public Map<String, LayerFunction> getSymbols() {
        return symbols;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setSymbols(Map<String, LayerFunction> symbols) {
        this.symbols = symbols;
    }

    public void addFunction(LayerFunction function) {
        symbols.put(function.getMangledName(), function);
    }


}
