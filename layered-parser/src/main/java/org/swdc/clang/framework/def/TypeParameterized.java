package org.swdc.clang.framework.def;

public class TypeParameterized {

    private String name;

    private AbstractNativeType type;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AbstractNativeType getType() {
        return type;
    }

    public void setType(AbstractNativeType type) {
        this.type = type;
    }

}
