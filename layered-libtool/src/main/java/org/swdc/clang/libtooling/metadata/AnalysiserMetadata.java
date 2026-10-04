package org.swdc.clang.libtooling.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysiserMetadata {

    private Map<String, BuiltInDescriptor> builtIn = new HashMap<>();

    private Map<String, FunctionDescriptor> functions = new HashMap<>();

    private Map<String, RecordDescriptor> structType = new HashMap<>();

    private Map<String, PointerDescriptor> pointers = new HashMap<>();

    private List<DiagnosticMsg> exceptions = new ArrayList<>();

    public List<DiagnosticMsg> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<DiagnosticMsg> exceptions) {
        this.exceptions = exceptions;
    }

    public Map<String, BuiltInDescriptor> getBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(Map<String, BuiltInDescriptor> builtIn) {
        this.builtIn = builtIn;
    }

    public void setFunctions(Map<String, FunctionDescriptor> functions) {
        this.functions = functions;
    }

    public Map<String, FunctionDescriptor> getFunctions() {
        return functions;
    }

    public Map<String, RecordDescriptor> getStructType() {
        return structType;
    }

    public void setStructType(Map<String, RecordDescriptor> structType) {
        this.structType = structType;
    }

    public Map<String, PointerDescriptor> getPointers() {
        return pointers;
    }

    public void setPointers(Map<String, PointerDescriptor> pointers) {
        this.pointers = pointers;
    }

    public AbstractDescriptor findDescriptor(String id) {
        if (builtIn.containsKey(id)) {
            return builtIn.get(id);
        }
        if (functions.containsKey(id)) {
            return functions.get(id);
        }
        if (structType.containsKey(id)) {
            return structType.get(id);
        }
        if (pointers.containsKey(id)) {
            return pointers.get(id);
        }
        return null;
    }
}
