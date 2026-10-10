package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

public class ShortPointer extends SeekablePointer{

    protected ShortPointer(Allocator allocator, long address, int alignment, boolean owner) {
        super(allocator, address, alignment, owner);
    }

    protected ShortPointer(Allocator allocator, long address, int alignment, boolean owner, int capacity) {
        super(allocator, address, alignment, owner, capacity);
    }

    protected ShortPointer(Allocator allocator, OpaquePointer source, long offset) {
        super(allocator, source, offset);
    }

    public ShortPointer(UniquePointer pointer) {
        super(pointer);
    }

    public short get(int index) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        return MemoryManager.readShort(getAddress(), index);
    }

    public void set(int index, short value) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        MemoryManager.writeShort(getAddress(), index, value);
    }

    public short[] getArray(int size) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        return MemoryManager.readShortArray(getAddress(), size);
    }

    public void setArray(short[] array) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        MemoryManager.writeShortArray(getAddress(), array);
    }

    @Override
    public int getElementMemorySize() {
        return MemoryManager.sizeOfShort();
    }

}
