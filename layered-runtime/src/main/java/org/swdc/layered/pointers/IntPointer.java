package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

import java.util.Arrays;

public class IntPointer extends SeekablePointer<Integer> {

    protected IntPointer(Allocator allocator,long address, int elementSize, int capacity, boolean aligned, boolean owner) {
        super(allocator,address, elementSize, capacity,aligned, owner);
    }

    protected IntPointer(Allocator allocator,IntPointer address, int offset) {
        super(allocator,address, offset);
    }

    public int get(int index) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }

        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new IndexOutOfBoundsException("index out of bounds");
        }

        return MemoryManager.readInt(getAddress(), index);
    }

    public void set(int index, Integer value) {

        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (index < 0 || index >= getCapacity()) {
            throw new IndexOutOfBoundsException("index out of bounds");
        }
        MemoryManager.writeInt(getAddress(), index, value);

    }


    public int[] getArray(int size) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (size < 0) {
            throw new IllegalArgumentException("invalid size");
        }
        if (getCapacity() > 0 && size > getCapacity()) {
            throw new IllegalArgumentException("size out of bounds");
        }

        return MemoryManager.readIntArray(getAddress(), size);
    }

    public void setArray(int[] values) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (values == null) {
            throw new IllegalArgumentException("invalid values");
        }
        if (getCapacity() > 0 && values.length > getCapacity()) {
            throw new IllegalArgumentException("values out of bounds");
        }
        MemoryManager.writeIntArray(getAddress(), values);

    }

    @Override
    public IntPointer at(int index) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }

        if (index < 0 || getCapacity() > 0 && index >= getCapacity()) {
            throw new IllegalArgumentException("index out of bounds");
        }

        return new IntPointer(getAllocator(),this, index);
    }

    public static IntPointer unmanaged(Allocator allocator, long address, int capacity) {
        return new IntPointer(allocator,address,MemoryManager.sizeOfInt(),capacity,false,false);
    }

}
