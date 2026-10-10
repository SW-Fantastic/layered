package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

public class LongPointer extends SeekablePointer {

    protected LongPointer(Allocator allocator, long address, int alignment, boolean owner) {
        super(allocator, address, alignment, owner);
    }

    protected LongPointer(Allocator allocator, long address, int alignment, boolean owner, int capacity) {
        super(allocator, address, alignment, owner, capacity);
    }

    protected LongPointer(Allocator allocator, OpaquePointer source, long offset) {
        super(allocator, source, offset);
    }

    public LongPointer(UniquePointer pointer) {
        super(pointer);
    }


    @Override
    public int getElementMemorySize() {
        return MemoryManager.sizeOfLong();
    }

    public long get(int index) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index out of bounds");
        }
        return MemoryManager.readLong(getAddress(), index);
    }

    public void set(int index, long value) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index out of bounds");
        }
        MemoryManager.writeLong(getAddress(), index, value);
    }

    public long[] getArray(int size) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        return MemoryManager.readLongArray(getAddress(), size);
    }

    public void setArray(long[] array) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        MemoryManager.writeLongArray(getAddress(), array);
    }

}
