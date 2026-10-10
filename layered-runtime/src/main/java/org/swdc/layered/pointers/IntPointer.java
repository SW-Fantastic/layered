package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

public class IntPointer extends SeekablePointer{


    protected IntPointer(Allocator allocator, long address, int alignment, boolean owner) {
        super(allocator, address, alignment, owner);
    }

    protected IntPointer(Allocator allocator, long address, int alignment, boolean owner, int capacity) {
        super(allocator, address, alignment, owner, capacity);
    }

    protected IntPointer(Allocator allocator, OpaquePointer source, long offset) {
        super(allocator,source, offset);
    }

    public IntPointer(UniquePointer pointer) {
        super(pointer);
    }

    @Override
    public int getElementMemorySize() {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        return MemoryManager.sizeOfInt();
    }

    public int get(int index) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && ( index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index out of bounds");
        }
        return MemoryManager.readInt(getAddress(), index);
    }

    public void set(int index, int value) {
        if (getCapacity() > 0 && ( index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index out of bounds");
        }
        MemoryManager.writeInt(getAddress(), index, value);
    }

    public int[] getArray(int size) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && ( size < 0 || size >= getCapacity())) {
            throw new RuntimeException("Size out of bounds");
        }
        return MemoryManager.readIntArray(getAddress(), size);
    }

    public void setArray(int[] array) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && ( array.length >= getCapacity())) {
            throw new RuntimeException("Size out of bounds");
        }
        MemoryManager.writeIntArray(getAddress(), array);
    }

}
