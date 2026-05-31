package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

public class ShortPointer extends SeekablePointer<Short> {

    protected ShortPointer(Allocator allocator, ShortPointer source, int offset) {
        super(allocator, source, offset);
    }

    protected ShortPointer(Allocator allocator, long address, int elementSize, int capacity, boolean aligned, boolean owner) {
        super(allocator, address, elementSize, capacity, aligned, owner);
    }

    public short get(int index) {

        if (isNull()) {
            throw new NullPointerException("Pointer is null");
        }
        if (getCapacity() > 0 && index >= getCapacity() || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Capacity: " + getCapacity());
        }
        return MemoryManager.readShort(getAddress(),index);

    }

    public void set(int index, short value) {

        if (isNull()) {
            throw new NullPointerException("Pointer is null");
        }
        if (getCapacity() > 0 && index >= getCapacity() || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Capacity: " + getCapacity());
        }
        MemoryManager.writeShort(getAddress(),index,value);

    }

    public short[] getArray(int size) {

        if (isNull()) {
            throw new NullPointerException("Pointer is null");
        }
        if (getCapacity() > 0 && size > getCapacity()) {
            throw new IndexOutOfBoundsException("Index: " + size + ", Capacity: " + getCapacity());
        }
        return MemoryManager.readShortArray(getAddress(),size);

    }

    public void setArray(short[] values) {

        if (isNull()) {
            throw new NullPointerException("Pointer is null");
        }
        if (getCapacity() > 0 && values.length > getCapacity()) {
            throw new IndexOutOfBoundsException("Index: " + values.length + ", Capacity: " + getCapacity());
        }
        MemoryManager.writeShortArray(getAddress(),values);

    }

    @Override
    public ShortPointer at(int index) {

        if (isNull()) {
            throw new NullPointerException("Pointer is null");
        }
        if (getCapacity() > 0 && index >= getCapacity() || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Capacity: " + getCapacity());
        }

        return new ShortPointer(getAllocator(),this, index);

    }

}
