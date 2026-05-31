package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

import java.util.stream.IntStream;

public class FloatPointer extends SeekablePointer<Float> {

    protected FloatPointer(Allocator allocator, FloatPointer address, int capacity) {
        super(allocator,address, capacity);
    }

    protected FloatPointer(Allocator allocator, long address, int elementSize, int capacity, boolean aligned, boolean owner) {
        super(allocator, address, elementSize, capacity, aligned, owner);
    }

    public float get(int index) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new IllegalArgumentException("index out of bounds");
        }

        return MemoryManager.readFloat(getAddress(), index);

    }

    public void set(int index, float value) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new IllegalArgumentException("index out of bounds");
        }
        MemoryManager.writeFloat(getAddress(), index, value);
    }

    public float[] getArray(int size) {
        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size is negative");
        }
        if (getCapacity() > 0 && size > getCapacity()) {
            throw new IllegalArgumentException("size is greater than capacity");
        }
        return MemoryManager.readFloatArray(getAddress(), size);
    }


    public void setArray(float[] values) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }

        if (values == null) {
            throw new IllegalArgumentException("values is null");
        }
        if (getCapacity() > 0 && values.length > getCapacity()) {
            throw new IllegalArgumentException("values length is greater than capacity");
        }

        MemoryManager.writeFloatArray(getAddress(), values);

    }

    @Override
    public FloatPointer at(int index) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new IllegalArgumentException("index out of bounds");
        }
        return new FloatPointer(getAllocator(),this,index);

    }

    public static FloatPointer unmanaged(Allocator allocator,long address, int capacity) {
        return new FloatPointer(allocator,address,MemoryManager.sizeOfFloat(),capacity,false,false);
    }

}
