package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

/**
 * Long型指针，请注意区分LP64和LLP64平台的区分，
 * 在 LP64 平台上，long 和 long long 是相同的，而在 LLP64 平台上，long 是32位而 long long 是64位的
 * Windows系统属于LLP64设计，因此此系统下long型的值其实本质上是int型，而long long才是64位，因此此处需要区分，
 * 此外，Java采用的是相当于LP64的设计，因此，Java的long应视为long long型，或者int64_t。
 *
 * 当Windows尝试向本指针装填超过Integer最大值的内容的时候，本类将会抛出异常。
 */
public class LongPointer extends SeekablePointer {

    protected LongPointer(Allocator allocator, long address, int alignment, boolean owner) {
        super(allocator, address, alignment, owner);
    }

    protected LongPointer(Allocator allocator, long address, int alignment, boolean owner, int capacity) {
        super(allocator, address, alignment, owner, capacity);
    }

    protected LongPointer(Allocator allocator, OpaquePointer source, long offset) {
        super(allocator, source, offset);
    }

    public LongPointer(UniquePointer pointer) {
        super(pointer);
    }


    @Override
    public int getElementMemorySize() {
        return MemoryManager.getSizeOfLong();
    }

    public long get(int index) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index out of bounds");
        }
        return MemoryManager.readLong(getAddress(), index);
    }

    /**
     * 将指定的 long 值写入指针指向的内存区域中第 index 个元素。
     * <p>Windows 平台上由于底层写入接口的限制，不允许写入超过 Integer.MAX_VALUE 的值。</p>
     *
     * @param index 元素下标，从 0 开始；当指针容量大于 0 时，下标必须落在容量范围内
     * @param value 待写入的 long 值
     * @throws RuntimeException 指针为空、下标越界，或 Windows 平台上 value 超过 Integer.MAX_VALUE 时抛出
     */
    public void set(int index, long value) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && (index < 0 || index >= getCapacity())) {
            throw new RuntimeException("Index out of bounds");
        }

        String osName = System.getProperty("os.name").toLowerCase();
        if (value > Integer.MAX_VALUE && osName.contains("win")) {
            throw new RuntimeException("value is too large for this platform");
        }
        MemoryManager.writeLong(getAddress(), index, value);
    }

    public long[] getArray(int size) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() > 0 && size < 0 || size >= getCapacity()) {
            throw new RuntimeException("Size out of bounds");
        }
        return MemoryManager.readLongArray(getAddress(), size);
    }

    /**
     * 将给定的 long 数组批量写入指针指向的内存区域。
     * <p>Windows 平台上由于底层写入接口的限制，不允许写入超过 Integer.MAX_VALUE 的元素值。</p>
     *
     * @param array 待写入的 long 数组，不可为 null
     * @throws RuntimeException 指针为空，或 Windows 平台上存在超过 Integer.MAX_VALUE 的元素时抛出
     */
    public void setArray(long[] array) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }

        String osName = System.getProperty("os.name").toLowerCase();
        for (long value : array) {
            if (value > Integer.MAX_VALUE && osName.contains("win")) {
                throw new RuntimeException("value is too large for this platform");
            }
        }

        MemoryManager.writeLongArray(getAddress(), array);

    }

}
