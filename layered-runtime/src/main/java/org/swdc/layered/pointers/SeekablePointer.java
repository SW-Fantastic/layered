package org.swdc.layered.pointers;

public abstract class SeekablePointer extends OpaquePointer {

    /**
     * 容量，即可以容纳的元素数量。
     */
    protected long capacity;

    protected SeekablePointer(Allocator allocator, long address, int alignment, boolean owner) {
        super(allocator, address, alignment, owner);
        this.capacity = 0;
    }

    protected SeekablePointer(Allocator allocator, long address, int alignment, boolean owner, int capacity) {
        super(allocator, address, alignment, owner);
        this.capacity = capacity;
    }

    protected SeekablePointer(Allocator allocator,OpaquePointer source, long offset) {
        super(allocator,source, offset);
    }

    public SeekablePointer(UniquePointer pointer) {
        super(pointer);
    }


    /**
     * 获取单个元素占用的内存大小。
     *
     * @return 单个元素的内存大小，单位为字节
     */
    public abstract int getElementMemorySize();

    /**
     * 获取容量，即可以容纳的元素数量。
     *
     * @return 元素的数量
     */
    public long getCapacity() {
        return capacity;
    }


}
