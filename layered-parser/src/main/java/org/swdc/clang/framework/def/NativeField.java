package org.swdc.clang.framework.def;

import org.swdc.layered.NativeAccessor;

public class NativeField extends TypeParameterized {

    private NativeAccessor accessor = NativeAccessor.PUBLIC;

    public NativeAccessor getAccessor() {
        return accessor;
    }

    public void setAccessor(NativeAccessor accessor) {
        this.accessor = accessor;
    }

    @Override
    public String toString() {
        return "NativeField{" +
                "name='" + getName() + '\'' +
                ", type=" + getType().getName() +
                '}';
    }

}
