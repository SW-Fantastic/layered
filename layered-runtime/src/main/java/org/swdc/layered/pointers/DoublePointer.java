package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

public class DoublePointer extends SeekablePointer {


    protected DoublePointer(Allocator allocator, long address, int alignment, boolean owner) {
        super(allocator, address, alignment, owner);
    }

    protected DoublePointer(Allocator allocator, long address, int alignment, boolean owner, int capacity) {
        super(allocator, address, alignment, owner, capacity);
    }

    protected DoublePointer(Allocator allocator, OpaquePointer source, long offset) {
        super(allocator, source, offset);
    }

    public DoublePointer(UniquePointer pointer) {
        super(pointer);
    }

    @Override
    public int getElementMemorySize() {
        return MemoryManager.sizeOfDouble();
    }

    public double get(int index) {
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index: " + index + ", Capacity: " + getCapacity());
        }
        return MemoryManager.readDouble(getAddress(), index);
    }

    public void set(int index, double value) {
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index: " + index + ", Capacity: " + getCapacity());
        }
        MemoryManager.writeDouble(getAddress(), index, value);
    }

    public double[] getArray(int size) {
        if (getCapacity() > 0 && (size < 0 || size >= getCapacity())) {
            throw new RuntimeException("Size: " + size + ", Capacity: " + getCapacity());
        }
        return MemoryManager.readDoubleArray(getAddress(), size);
    }

    public void setArray(double[] array) {
        if (getCapacity() > 0 && (array.length >= getCapacity())) {
            throw new RuntimeException("Size: " + array.length + ", Capacity: " + getCapacity());
        }
        MemoryManager.writeDoubleArray(getAddress(), array);
    }

}
