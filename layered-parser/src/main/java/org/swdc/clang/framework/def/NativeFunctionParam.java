package org.swdc.clang.framework.def;

public class NativeFunctionParam extends TypeParameterized {

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数索引位置，从0开始。
     */
    private int index;

    /**
     * 参数类型，例如：int, float等。
     */
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
