package org.swdc.clang.framework.def;

public class NativeField extends TypeParameterized {

    @Override
    public String toString() {
        return "NativeField{" +
                "name='" + getName() + '\'' +
                ", type=" + getType().getName() +
                '}';
    }

}
