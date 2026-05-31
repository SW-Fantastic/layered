package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

/**
 * 布尔型指针，指向一个用于存储Bool的内存空间
 */
public class BooleanPointer extends SeekablePointer<Boolean> {

    protected BooleanPointer(Allocator allocator, BooleanPointer source, int offset) {
        super(allocator, source, offset);
    }

    protected BooleanPointer(Allocator allocator, long address, int elementSize, int capacity, boolean aligned, boolean owner) {
        super(allocator, address, elementSize, capacity, aligned, owner);
    }

    public boolean get(int index) {
        if (isNull()) {
            throw new NullPointerException("Pointer is null !");
        }
        if (getCapacity() > 0 && index > getCapacity() || index < 0) {
            throw new IllegalArgumentException("Index: " + index + ", Capacity: " + getCapacity());
        }
        return MemoryManager.readBoolean(getAddress(),index);
    }

    public void set(int index, boolean value) {
        if (isNull()) {
            throw new NullPointerException("Pointer is null !");
        }
        if (getCapacity() > 0 && index > getCapacity() || index < 0) {
            throw new IllegalArgumentException("Index: " + index + ", Capacity: " + getCapacity());
        }
        MemoryManager.writeBoolean(getAddress(),index,value);
    }

    public boolean[] getArray(int size) {

        if (isNull()) {
            throw new NullPointerException("Pointer is null !");
        }
        if (size <= 0 || getCapacity() > 0 && size > getCapacity()) {
            throw new IllegalArgumentException("Size: " + size + ", Capacity: " + getCapacity());
        }
        return MemoryManager.readBooleanArray(getAddress(),size);

    }

    public void setArray(boolean[] array) {

        if (isNull()) {
            throw new NullPointerException("Pointer is null !");
        }
        if (getCapacity() > 0 && array.length > getCapacity()) {
            throw new IllegalArgumentException("Size: " + array.length + ", Capacity: " + getCapacity());
        }
        MemoryManager.writeBooleanArray(getAddress(),array);

    }

    @Override
    public BooleanPointer at(int index) {

        if (isNull()) {
            throw new NullPointerException("pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new IllegalArgumentException("index out of bounds");
        }
        return new BooleanPointer(getAllocator(),this,index);

    }

    public static BooleanPointer unmanaged(Allocator allocator, long address, int capacity) {
        return new BooleanPointer(allocator,address,MemoryManager.sizeOfBoolean(),capacity,false,false);
    }

}
