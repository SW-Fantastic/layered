package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

public class FloatPointer extends SeekablePointer {


    protected FloatPointer(Allocator allocator, long address, int alignment, boolean owner) {
        super(allocator, address, alignment, owner);
    }

    protected FloatPointer(Allocator allocator, long address, int alignment, boolean owner, int capacity) {
        super(allocator, address, alignment, owner, capacity);
    }

    protected FloatPointer(Allocator allocator, OpaquePointer source, long offset) {
        super(allocator, source, offset);
    }

    public FloatPointer(UniquePointer pointer) {
        super(pointer);
    }

    @Override
    public int getElementMemorySize() {
        return MemoryManager.sizeOfFloat();
    }

    public float get(int index) {
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index: " + index + ", Capacity: " + getCapacity());
        }
        return MemoryManager.readFloat(getAddress(), index);
    }

    public void set(int index, float value) {
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index: " + index + ", Capacity: " + getCapacity());
        }
        MemoryManager.writeFloat(getAddress(), index, value);
    }

    public float[] getArray(int size) {
        if (getCapacity() > 0 && (size < 0 || size >= getCapacity())) {
            throw new RuntimeException("Size: " + size + ", Capacity: " + getCapacity());
        }
        return MemoryManager.readFloatArray(getAddress(), size);
    }

    public void setArray(float[] array) {
        if (getCapacity() > 0 && (array.length >= getCapacity())) {
            throw new RuntimeException("Size: " + array.length + ", Capacity: " + getCapacity());
        }
        MemoryManager.writeFloatArray(getAddress(), array);
    }

}
