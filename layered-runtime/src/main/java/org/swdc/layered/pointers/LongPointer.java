package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

import java.util.Arrays;

public class LongPointer extends SeekablePointer<Long> {

    protected LongPointer(Allocator allocator, SeekablePointer source, int offset) {
        super(allocator,source, offset);
    }

    protected LongPointer(Allocator allocator,long address, int elementSize, int capacity, boolean aligned, boolean owner) {
        super(allocator,address, elementSize, capacity, aligned, owner);
    }

    public Long get(int index) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new IllegalArgumentException("index out of bounds");
        }
        return MemoryManager.readLong(getAddress(), index);

    }


    public void set(int index, long value) {
        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new IllegalArgumentException("index out of bounds");
        }
        MemoryManager.writeLong(getAddress(), index, value);
    }


    public long[] getArray(int size) {
        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (size < 0) {
            throw new IllegalArgumentException("invalid size");
        }
        if (getCapacity() > 0 && size > getCapacity()) {
            throw new IllegalArgumentException("size out of bounds");
        }
        return MemoryManager.readLongArray(getAddress(), size);

    }

    public void setArray(long[] values) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }

        if (values == null) {
            throw new IllegalArgumentException("invalid values");
        }

        if (getCapacity() > 0 && values.length > getCapacity()) {
            throw new IllegalArgumentException("values out of bounds");
        }

        MemoryManager.writeLongArray(getAddress(),values);

    }

    @Override
    public LongPointer at(int index) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }

        if (index < 0 || getCapacity() > 0 && index >= getCapacity()) {
            throw new IllegalArgumentException("index out of bounds");
        }

        return new LongPointer(getAllocator(),this, index);

    }

    public static LongPointer unmanaged(Allocator allocator, long address, int capacity) {
        return new LongPointer(allocator,address,MemoryManager.sizeOfLong(),capacity,false,false);
    }

}
