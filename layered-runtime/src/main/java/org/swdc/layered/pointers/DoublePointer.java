package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

import java.util.Arrays;

/**
 * 指向Double类型的指针。
 */
public class DoublePointer extends SeekablePointer<Double> {

    protected DoublePointer(Allocator allocator, SeekablePointer source, int offset) {
        super(allocator,source, offset);
    }

    protected DoublePointer(Allocator allocator, long address, int elementSize, int capacity, boolean aligned, boolean owner) {
        super(allocator,address, elementSize, capacity, aligned, owner);
    }

    public Double get(int index) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new IllegalArgumentException("index out of bounds");
        }

        return MemoryManager.readDouble(getAddress(), index);

    }

    public void set(int index, Double value) {

        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new IllegalArgumentException("index out of bounds");
        }
        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        MemoryManager.writeDouble(getAddress(), index, value);

    }

    public void setArray(double[] values) {

        if (values == null) {
            throw new IllegalArgumentException("values is null");
        }

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }

        if (getCapacity() > 0 && values.length > getCapacity()) {
            throw new IllegalArgumentException("index out of bounds");
        }

        MemoryManager.writeDoubleArray(getAddress(), values);

    }

    public double[] getArray(int size) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }

        if (size < 0) {
            throw new IllegalArgumentException("invalid size");
        }
        if (getCapacity() > 0 && size > getCapacity()) {
            throw new IllegalArgumentException("size out of bounds");
        }

        return MemoryManager.readDoubleArray(getAddress(), size);
    }

    @Override
    public DoublePointer at(int index) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }

        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new IllegalArgumentException("index out of bounds");
        }

        return new DoublePointer(getAllocator(), this, index);

    }

    public static DoublePointer unmanaged(Allocator allocator, long address, int capacity) {
        return new DoublePointer(allocator,address,MemoryManager.sizeOfDouble(),capacity,false,false);
    }

}
