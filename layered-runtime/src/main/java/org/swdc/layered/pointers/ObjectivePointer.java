package org.swdc.layered.pointers;

public abstract class ObjectivePointer extends OpaquePointer {

    protected ObjectivePointer(Allocator allocator, long address, boolean owner) {

        initPointer(allocator,address,owner);

    }

}
