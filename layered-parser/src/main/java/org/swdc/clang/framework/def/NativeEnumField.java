package org.swdc.clang.framework.def;

public class NativeEnumField extends NativeField {

    private long value;

    public NativeEnumField() {
        super.setType(BasicType.LONG);
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public void setType(AbstractNativeType type) {
        throw new UnsupportedOperationException("EnumFieldType type cannot be changed");
    }

    @Override
    public String toString() {
        return "EnumNativeField{ name=" + getName() + ", value=" + value + "}";
    }
}
