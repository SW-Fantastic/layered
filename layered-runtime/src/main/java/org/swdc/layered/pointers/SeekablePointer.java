package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

/**
 * 基础指针，也是指针的抽象类。
 * 封装了一个内存地址用于操作本地函数和类库。
 *
 * 指针的分配，既是内存的分配，同时也是资源的分配。
 * 从零开始，不依赖任何其他指针或者本地对象创建一个新的指针类型对象，实际上
 * 是内存的分配，在指针的生命周期内，它将持有这块内存的所有权。
 *
 * 指针的所有权既是释放内存空间的权力，如果指针持有所有权，它应该通过MemoryManager释放内存，
 * 否则，它应当归还引用计数，直到所有的引用计数都被归还，它的所有者才可以真正是释放内存资源。
 *
 * 如果一个指针不是你申请（allocate/malloc）的，那么你不应该释放它，否则，务必通过free方法
 * 释放资源（即使它是通过at方法创建的子指针）。
 *
 * @param <T>
 */
public class SeekablePointer<T> extends OpaquePointer {

    /**
     * 本指针的容量，表达可以容纳多少个ElementSize的元素，
     * 类似于数组的length。
     */
    private int capacity = 0;

    /**
     * 本指针的元素大小，类似于数组中每个元素的字节数。
     */
    private int elementSize = 0;

    /**
     * 是否为对齐的内存。
     */
    private boolean aligned = false;


    /**
     * 指针的偏移构造方法，通过一个指针的偏移量构造一个新的指针。
     * @param source       指针的所有者
     * @param offset       指针的偏移位置
     */
    protected SeekablePointer(Allocator allocator, SeekablePointer source, int offset) {

        if (source == null || source.isNull()) {
            throw new NullPointerException("source pointer is null or invalid.");
        }
        if (allocator == null) {
            throw new NullPointerException("allocator is null or invalid.");
        }

        long address = source.getAddress() + (long) offset * source.getElementSize();
        this.initPointer(allocator, address, source);

        this.elementSize = source.getElementSize();
        this.aligned = source.isAligned();
        if (source.capacity > 0) {
            this.capacity = (source.getCapacity() - offset);
        }

    }


    /**
     * 正常的指针构造方法，通过指针的元数据构造一个指针。
     * @param address       指针的地址
     * @param elementSize   指针的元素大小
     * @param capacity      指针的容量
     * @param aligned       是否为对齐的内存
     * @param owner         指针是否为内存的分配者
     */
    protected SeekablePointer(Allocator allocator, long address, int elementSize, int capacity, boolean aligned, boolean owner) {

        this.initPointer(allocator,address,owner);
        this.elementSize = elementSize;
        this.capacity = capacity;
        this.aligned = aligned;

    }


    public int getElementSize() {
        return elementSize;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isAligned() {
        return aligned;
    }

    public SeekablePointer at(int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected void deAllocate() {
        if (!isOwner()) {
            return;
        }
        getAllocator().free(getAddress(), isAligned());
    }

    public boolean clear() {

        if(isNull()) {
            throw new NullPointerException("can not clean a null pointer");
        }

        if (capacity <= 0) {
            throw new IllegalStateException("capacity must be greater than 0");
        }

        return MemoryManager.memset(getAddress(),0, (long) elementSize * capacity);
    }



}
