package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 内存分配器
 * 申请内存，释放内存，以及各类指针操作。
 */
public class Allocator {

    private Map<Long, OpaquePointer> allocated = new HashMap<>();

    private Map<Long, OpaquePointer> subPointers = new HashMap<>();

    OpaquePointer getAllocated(long address) {
        return allocated.get(address);
    }

    void ref(OpaquePointer pointer) {
        if (allocated.containsKey(pointer.getAddress())) {
            return;
        }
        subPointers.put(pointer.getAddress(), pointer);
    }

    void unref(OpaquePointer pointer) {
        if (allocated.containsKey(pointer.getAddress())) {
            return;
        }
        subPointers.remove(pointer.getAddress());
    }

    /**
     * 申请指向另一个指针的指针。
     * @param capacity 需要容纳多少个指针
     * @return 申请到的指针对象。
     */
    public AddressPointer allocateAddress(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal capacity: " + capacity);
        }

        int sizeOf = MemoryManager.sizeOfPointer();
        int totalSize = sizeOf * capacity;
        long addr = MemoryManager.malloc(totalSize);
        if (addr == 0) {
            throw new IllegalArgumentException("Failed to malloc memory");
        }
        AddressPointer pointer = new AddressPointer<>(this,addr,sizeOf,capacity,false,true);
        pointer.clear();
        allocated.put(addr,pointer);
        return pointer;

    }

    /**
     * 申请整形指针。
     * @param capacity
     * @return
     */
    public IntPointer allocateInt(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }

        int sizeOf = MemoryManager.sizeOfInt();
        int totalSize = sizeOf *  capacity;
        long addr = MemoryManager.malloc(totalSize);
        if (addr == 0) {
            throw new IllegalStateException("Failed to allocate memory");
        }
        IntPointer pointer = new IntPointer(this,addr,sizeOf,capacity,false,true);
        pointer.clear();
        allocated.put(addr, pointer);
        return pointer;

    }

    /**
     * 申请浮点型指针
     * @param capacity
     * @return
     */
    public FloatPointer allocateFloat(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }

        int sizeOf = MemoryManager.sizeOfFloat();
        int totalSize = sizeOf *  capacity;
        long addr = MemoryManager.malloc(totalSize);
        if (addr == 0) {
            throw new IllegalStateException("Failed to allocate memory");
        }
        FloatPointer pointer = new FloatPointer(this,addr,sizeOf,capacity,false,true);
        pointer.clear();
        allocated.put(addr, pointer);
        return pointer;

    }

    /**
     * 申请双精度指针
     * @param capacity
     * @return
     */
    public DoublePointer allocateDouble(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }

        int sizeOf = MemoryManager.sizeOfDouble();
        int totalSize = sizeOf *  capacity;
        long addr = MemoryManager.malloc(totalSize);
        if (addr == 0) {
            throw new IllegalStateException("Failed to allocate memory");
        }

        DoublePointer pointer = new DoublePointer(this,addr,sizeOf,capacity,false,true);
        pointer.clear();
        allocated.put(addr, pointer);
        return pointer;

    }

    /**
     * 申请短整数型指针
     * @param capacity
     * @return
     */
    public ShortPointer allocateShort(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }

        int sizeOf = MemoryManager.sizeOfShort();
        int totalSize = sizeOf *  capacity;
        long addr = MemoryManager.malloc(totalSize);
        if (addr == 0) {
            throw new IllegalStateException("Failed to allocate memory");
        }
        ShortPointer pointer = new ShortPointer(this,addr,sizeOf,capacity,false,true);
        pointer.clear();
        allocated.put(addr, pointer);
        return pointer;

    }

    /**
     * 申请长整数型指针
     * @param capacity
     * @return
     */
    public LongPointer allocateLong(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        int sizeOf = MemoryManager.sizeOfLong();
        int totalSize = sizeOf *  capacity;
        long addr = MemoryManager.malloc(totalSize);
        if (addr == 0) {
            throw new IllegalStateException("Failed to allocate memory");
        }
        LongPointer pointer = new LongPointer(this,addr,sizeOf,capacity,false,true);
        pointer.clear();
        allocated.put(addr, pointer);
        return pointer;
    }

    /**
     * 申请布尔型指针。
     * @param capacity
     * @return
     */
    public BooleanPointer allocateBoolean(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        int sizeOf = MemoryManager.sizeOfBoolean();
        int totalSize = sizeOf * capacity;
        long addr = MemoryManager.malloc(totalSize);
        if (addr == 0) {
            throw new IllegalStateException("Failed to allocate memory");
        }
        BooleanPointer pointer = new BooleanPointer(this,addr,sizeOf,capacity,false,true);
        pointer.clear();
        allocated.put(addr, pointer);
        return pointer;

    }

    /**
     * 申请字符型指针（二进制数据型指针）
     * @param capacity
     * @return
     */
    public BytePointer allocateByte(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        int sizeOf = MemoryManager.sizeOfByte();
        int totalSize = sizeOf * capacity;
        long addr = MemoryManager.malloc(totalSize);
        if (addr == 0) {
            throw new IllegalStateException("Failed to allocate memory");
        }
        BytePointer pointer = new BytePointer(this,addr,sizeOf,capacity,false,true);
        pointer.clear();
        allocated.put(addr, pointer);
        return pointer;

    }

    public BytePointer allocateByte(String string) {

        if (string == null) {
            throw new IllegalArgumentException("string must not be null");
        }
        if (string.isBlank()) {
            string = "\0";
        }
        BytePointer allocated = allocateByte(string.length() * MemoryManager.sizeOfByte());
        allocated.setString(string);
        return allocated;

    }

    public <T> OpaquePointer allocateByType(Class<T> type, int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }

        if (type == int.class || type == Integer.class) {
            int minCapacity = MemoryManager.sizeOfPointer() / MemoryManager.sizeOfInt();
            return allocateInt(Math.max(capacity, minCapacity));
        } else if (type == long.class || type == Long.class) {
            int minCapacity = MemoryManager.sizeOfPointer() / MemoryManager.sizeOfLong();
            return allocateLong(Math.max(capacity,minCapacity));
        } else if (type == short.class || type == Short.class) {
            int minCapacity = MemoryManager.sizeOfPointer() / MemoryManager.sizeOfShort();
            return allocateShort(Math.max(capacity,minCapacity));
        } else if (type == byte.class || type == Byte.class) {
            return allocateByte(capacity);
        } else if (type == float.class || type == Float.class) {
            int minCapacity = MemoryManager.sizeOfPointer() / MemoryManager.sizeOfFloat();
            return allocateFloat(Math.max(capacity,minCapacity));
        } else if (type == double.class || type == Double.class) {
            return allocateDouble(capacity);
        } else if (type == char.class || type == Character.class) {
            return allocateByte(capacity);
        } else if (type == boolean.class || type == Boolean.class) {
            return allocateBoolean(capacity);
        } else if (type == void.class || type == Void.class) {
            return allocateNull();
        } else if (type == String.class) {
            return allocateByte(capacity);
        } else if (OpaquePointer.class.isAssignableFrom(type)) {
            return allocateAddress(capacity);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }

    }

    /**
     * 申请空指针，占位用。
     * @return
     */
    public BytePointer allocateNull() {
        return new BytePointer(this,0,0,0,false,true);
    }

    boolean free(long address, boolean aligned) {
        if (address == 0L || !allocated.containsKey(address)) {
            return false;
        }
        if (aligned) {
            MemoryManager.freeAligned(address);
        } else {
            MemoryManager.free(address);
        }
        allocated.remove(address);
        return true;
    }

    public void free() {

        for (OpaquePointer subPointer : subPointers.values()) {
            subPointer.free();
        }
        subPointers.clear();

        for (OpaquePointer allocatedPointer : allocated.values()) {
            allocatedPointer.free();
        }
        allocated.clear();

    }

}
