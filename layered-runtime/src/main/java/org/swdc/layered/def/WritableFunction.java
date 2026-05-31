package org.swdc.layered.def;


import java.util.ArrayList;
import java.util.List;

public class WritableFunction extends Writeable {

    private List<WritableParameter> parameters = new ArrayList<>();

    private NativeDefinition returnType;

    private boolean constructor;

    private boolean destructor;

    private boolean method;

    private String symbolName;

    public String getSymbolName() {
        return symbolName;
    }

    public void setSymbolName(String symbolName) {
        this.symbolName = symbolName;
    }

    public void setConstructor(boolean constructor) {
        this.constructor = constructor;
    }

    public boolean isConstructor() {
        return constructor;
    }

    public void setDestructor(boolean destructor) {
        this.destructor = destructor;
    }

    public boolean isDestructor() {
        return destructor;
    }

    public boolean isMethod() {
        return method;
    }

    public void setMethod(boolean method) {
        this.method = method;
    }

    public List<WritableParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<WritableParameter> parameters) {
        this.parameters = parameters;
    }

    public NativeDefinition getReturnType() {
        return returnType;
    }

    public void setReturnType(NativeDefinition returnType) {
        this.returnType = returnType;
    }
}
