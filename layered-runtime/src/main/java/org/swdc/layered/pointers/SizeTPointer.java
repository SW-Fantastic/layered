package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

/**
 * 封装了对 size_t 类型指针的操作，
 * size_t类型是与编译器相关的类型，它通常是 32 位或 64 位的无符号整数，
 * 操作系统和编译器不同，其大小可能不同。
 *
 * 本类型是无符号的，因此不允许存入负值。
 */
public class SizeTPointer extends SeekablePointer {

    protected SizeTPointer(Allocator allocator, long address, int alignment, boolean owner) {
        super(allocator, address, alignment, owner);
    }

    protected SizeTPointer(Allocator allocator, long address, int alignment, boolean owner, int capacity) {
        super(allocator, address, alignment, owner, capacity);
    }

    protected SizeTPointer(Allocator allocator, OpaquePointer source, long offset) {
        super(allocator, source, offset);
    }

    public SizeTPointer(UniquePointer pointer) {
        super(pointer);
    }

    @Override
    public int getElementMemorySize() {
        return MemoryManager.getSizeOfSizeT();
    }
    
    /**
     * 读取指定索引处的 size_t 元素
     *
     * @param index 元素索引，从 0 开始
     * @return 该索引处的 size_t 值
     * @throws RuntimeException 指针为空时抛出
     */
    public long get(int index) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        return MemoryManager.readSizeT(getAddress(), index);
    }
    
    /**
     * 将指定索引处的 size_t 元素设置为给定的值
     *
     * <p>写入前会依次校验：指针非空、索引未越界（仅在容量大于 0 时校验）、值非负。</p>
     *
     * @param index 元素索引，从 0 开始
     * @param value 要写入的值，必须为非负数
     * @throws RuntimeException 指针为空、索引越界或值为负数时抛出
     */
    public void set(int index, long value) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && index >= getCapacity()) {
            throw new RuntimeException("Index out of bounds");
        }
        if (value < 0) {
            throw new RuntimeException("size_t cannot be negative");
        }
        MemoryManager.writeSizeT(getAddress(), index, value);
    }
    
    /**
     * 从指针位置读取指定数量的 size_t 元素
     *
     * <p>读取前会校验指针非空。</p>
     *
     * @param size 要读取的元素数量
     * @return 包含读取结果的 long 数组
     * @throws RuntimeException 指针为空时抛出
     */
    public long[] getArray(int size) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        return MemoryManager.readSizeTArray(getAddress(), size);
    }
    
    /**
     * 从指定索引处开始写入 long 数组作为 size_t 元素
     *
     * <p>写入前会依次校验：指针非空、数组非空、索引未越界、数组所有元素非负。</p>
     *
     * @param index 起始索引，从 0 开始
     * @param value 要写入的 long 数组，所有元素必须为非负数
     * @throws RuntimeException 指针为空、数组为空、索引越界或包含负数元素时抛出
     */
    public void setArray(int index, long[] value) {
        
        if (isNull() || value == null) {
            throw new RuntimeException("Pointer is null");
        }
        
        if (getCapacity() > 0 && index + value.length >= getCapacity()) {
            throw new RuntimeException("Index out of bounds");
        }
        
        for (long val : value) {
            if (val < 0) {
                throw new RuntimeException("size_t cannot be negative");
            }
        }
        
        MemoryManager.writeSizeTArray(getAddress(), value);
    }
    
}
