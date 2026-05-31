package org.swdc.clang.framework.def;

public class NativeConstant {

    private String name;

    private AbstractNativeType type;


    public AbstractNativeType getType() {
        return type;
    }

    public void setType(AbstractNativeType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
