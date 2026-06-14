package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

import java.lang.reflect.Constructor;

/**
 * 指针型指针，用于存储一个内存地址。
 * @param <T> 任意的其他指针
 */
public class AddressPointer<T extends OpaquePointer> extends SeekablePointer<T> {

    protected AddressPointer(Allocator allocator, AddressPointer source, int offset) {
        super(allocator,source, offset);
    }

    protected AddressPointer(Allocator allocator, long address, int elementSize, int capacity, boolean aligned, boolean owner) {
        super(allocator, address, elementSize, capacity, aligned, owner);
    }

    public OpaquePointer get(int index) {

        checkAddress(index);

        long addr = MemoryManager.readAddress(getAddress(), index);
        if (addr < 0) {
            return null;
        }
        OpaquePointer allocated = getAllocator().getAllocated(addr);
        if (allocated == null) {
            OpaquePointer pointer = new OpaquePointer();
            pointer.initPointer(getAllocator(),addr,false);
            return pointer;
        }
        return allocated;

    }

    public  T get(Class<T> type) {
        return get(0, type);
    }

    public  T get(int index, Class<T> type) {

        checkAddress(index);

        long addr = MemoryManager.readAddress(getAddress(), index);
        if (addr == 0) {
            return null;
        }
        OpaquePointer allocated = getAllocator().getAllocated(addr);
        if (allocated == null) {
            OpaquePointer pointer = new OpaquePointer();
            pointer.initPointer(getAllocator(),addr,false);
            return pointer.reinterrupt(type);
        } else {
            return allocated.as(type);
        }

    }

    public OpaquePointer[] getArray(int size) {

        checkAddress(size);

        OpaquePointer[] pointers = new OpaquePointer[size];
        long[] result = MemoryManager.readAddressArray(getAddress(), size);
        for (int i = 0; i < result.length; i++) {

            long addr = result[i];
            OpaquePointer pointer = getAllocator().getAllocated(addr);
            if (pointer == null && result[i] != 0) {
                pointer = new OpaquePointer();
                pointer.initPointer(getAllocator(),addr,false);
                pointers[i] = pointer;
            } else if (pointer != null) {
                pointers[i] = pointer;
            }

        }

        return pointers;
    }

    public  T[] getArray(Class<T> type, int size) {

        checkAddress(size);

        T[] pointers = (T[]) new Object[size];
        long[] result = MemoryManager.readAddressArray(getAddress(), size);
        for (int i = 0; i < result.length; i++) {
            long addr = result[i];
            OpaquePointer pointer = getAllocator().getAllocated(addr);
            if (pointer == null && result[i] != 0) {
                pointers[i] = get(i,type);
            } else if (pointer != null) {
                pointers[i] = pointer.as(type);
            }
        }

        return pointers;
    }

    public void set(int index, OpaquePointer value) {

        checkAddress(index);

        if (value == null) {
            throw new NullPointerException();
        }
        long pointerAddr = value.getAddress();
        MemoryManager.writeAddress(getAddress(), index, pointerAddr);

    }

    public void setArray(OpaquePointer[] values) {

        if (values == null) {
            throw new IllegalArgumentException("Cannot set null array");
        }
        checkAddress(values.length);
        long[] pointers = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            pointers[i] = values[i].getAddress();
        }
        MemoryManager.writeAddressArray(getAddress(), pointers);

    }

    public void checkAddress(int position) {
        if (isNull()) {
            throw new NullPointerException("Pointer is null");
        }
        if (position < 0 || getCapacity() > 0 && position > getCapacity()) {
            throw new IllegalArgumentException("Index out of bounds");
        }
    }


    public static AddressPointer unmanaged(Allocator allocator, long address, int capacity) {
        return new AddressPointer(allocator,address,MemoryManager.sizeOfPointer(),capacity,false,false);
    }


}
