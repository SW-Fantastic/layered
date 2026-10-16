package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

/**
 * Long long类型指针，封装了底层对 long long 类型数据的读写操作。
 * 在LP64和LLP64体系中，long long类型都是64位的标准长整型数据，
 * Java的语言体系中，long型的长度是64位，与long long类型兼容。
 */
public class LongLongPointer extends SeekablePointer {


    protected LongLongPointer(Allocator allocator, long address, int alignment, boolean owner) {
        super(allocator, address, alignment, owner);
    }

    protected LongLongPointer(Allocator allocator, long address, int alignment, boolean owner, int capacity) {
        super(allocator, address, alignment, owner, capacity);
    }

    protected LongLongPointer(Allocator allocator, OpaquePointer source, long offset) {
        super(allocator, source, offset);
    }

    public LongLongPointer(UniquePointer pointer) {
        super(pointer);
    }

    @Override
    public int getElementMemorySize() {
        return MemoryManager.sizeOfLongLong();
    }

    public long get(int index) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index out of bounds");
        }
        return MemoryManager.readLongLong(getAddress(), index);
    }

    public void set(int index, long value) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index out of bounds");
        }
        MemoryManager.writeLongLong(getAddress(), index, value);
    }

    /**
     * 从当前指针地址读取指定数量的 long long 数据，并返回为 long 数组。
     *
     * @param size 要读取的元素个数
     * @return 读取到的 long 数组
     * @throws RuntimeException 指针为 null 或读取大小越界时抛出
     */
    public long[] getArray(int size) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && size < 0 || size >= getCapacity()) {
            throw new RuntimeException("Size out of bounds");
        }
        return MemoryManager.readLongLongArray(getAddress(), size);
    }

    /**
     * 将 long 数组写入当前指针地址指向的连续 long long 内存区域。
     *
     * @param values 要写入的 long 数组
     * @throws RuntimeException 指针为 null 或写入长度越界时抛出
     */
    public void setArray(long[] values) {

        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }

        if (getCapacity() > 0 && values.length >= getCapacity()) {
            throw new RuntimeException("Index out of bounds");
        }
        MemoryManager.writeLongLongArray(getAddress(), values);

    }


}
