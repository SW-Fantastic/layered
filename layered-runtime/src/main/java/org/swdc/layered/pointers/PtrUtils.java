package org.swdc.layered.pointers;

public class PtrUtils {

    public static OpaquePointer getRawPointer(OpaquePointer ptr) {

        if (ptr == null) {
            throw new IllegalArgumentException("ptr is null");
        } else if(ptr.isNull()) {
            throw new IllegalArgumentException("ptr is empty pointer");
        }

        if (ptr.getSource() == null) {
            return ptr;
        }
        OpaquePointer raw = ptr;
        while (raw.getSource() != null) {
            raw = raw.getSource();
        }
        return raw;

    }

}
