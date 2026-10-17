package org.swdc.layered;

import java.util.HashMap;
import java.util.Map;

public class LayerMetadata {

    private Map<String, LayerFunction> symbols = new HashMap<>();

    private Map<String, LayerFunction> callbacks = new HashMap<>();

    public Map<String, LayerFunction> getSymbols() {
        return symbols;
    }

    public void setSymbols(Map<String, LayerFunction> symbols) {
        this.symbols = symbols;
    }

    public Map<String, LayerFunction> getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(Map<String, LayerFunction> callbacks) {
        this.callbacks = callbacks;
    }

}
