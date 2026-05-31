package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 二进制类型数据指针，内容是byte类型的，可以存储多种数据。
 */
public class BytePointer extends SeekablePointer<Character> {

    protected BytePointer(Allocator allocator, BytePointer source, int offset) {
        super(allocator, source, offset);
    }

    protected BytePointer(Allocator allocator, long address, int elementSize, int capacity, boolean aligned, boolean owner) {
        super(allocator, address, elementSize, capacity, aligned, owner);
    }

    public byte[] getBytes() {
        if (isNull()) {
            throw new NullPointerException("Pointer is null");
        }
        if (getCapacity() > 0) {
            return getBytes(0, getCapacity());
        }
        throw new IllegalArgumentException("Using get(int size) instead.");
    }

    public byte[] getBytes(int size) {
        return getBytes(0, size);
    }

    public byte[] getBytes(int offset, int size) {

        if (size < 0 || getCapacity() > 0 && size > getCapacity()) {
            throw new IllegalArgumentException("Invalid byte array size (" + size + ") given");
        }

        if (isNull()) {
            throw new NullPointerException("Null pointer is not allowed");
        }

        return MemoryManager.readByteArray(getAddress(), offset, size);
    }

    public byte getByte(int index) {
        if (index < 0 || getCapacity() > 0 && index >= getCapacity()) {
            throw new IllegalArgumentException("Invalid byte array index (" + index + ") given");
        }

        if (isNull()) {
            throw new NullPointerException("Null pointer is not allowed");
        }
        return MemoryManager.readByte(getAddress(), index);
    }

    public void setByte(byte b, int index) {

        if (isNull()) {
            throw new NullPointerException("Null pointer is not allowed");
        }

        if (getCapacity() > 0 && index > getCapacity() || index < 0) {
            throw new IllegalArgumentException("Invalid byte array size (" + index + ") given");
        }

        MemoryManager.writeByte(getAddress(), index, b);
    }

    public void setBytes(byte[] b) {

        setBytes(b, 0, 0,b.length);

    }

    public void setBytes(byte[] b, int offsetDst) {

        setBytes(b, 0, offsetDst,b.length);

    }

    public void setBytes(byte[] b, int offsetSrc, int offsetDst) {

        setBytes(b, offsetSrc, offsetDst,b.length);

    }

    public void setBytes(byte[] b, int srcOffset, int dstOffset, int size) {

        if (b == null) {
            throw new NullPointerException("Null pointer is not allowed");
        }

        if (srcOffset + size > b.length) {
            throw new IllegalArgumentException("Invalid byte array size (" + size + ") given");
        }

        if(getCapacity() > 0 && size > getCapacity()) {
            throw new IllegalArgumentException("Invalid byte array size (" + size + ") given");
        }

        if (getCapacity() > 0 && dstOffset + size > getCapacity()) {
            throw new IllegalArgumentException("Invalid byte array size (" + size + ") given");
        }

        if (isNull()) {
            throw new NullPointerException("Null pointer is not allowed");
        }

        MemoryManager.writeByteArray(getAddress(),dstOffset,b,srcOffset,size);

    }

    public String getString() {
        return getString(StandardCharsets.UTF_8);
    }


    public String getString(Charset charset)  {

        if (isNull()) {
            throw new NullPointerException("Null pointer is not allowed");
        }

        return new String(getBytes(), charset);

    }

    public String getString(String charset) {

        if (isNull()) {
            throw new NullPointerException("Null pointer is not allowed");
        }

        long strlen = MemoryManager.strlen(getAddress());
        if (strlen > 0) {
            try {
                byte[] data = getBytes((int)strlen);
                return new String(data, charset);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;

    }

    public void setString(String data) {

        if (isNull()) {
            throw new NullPointerException("Null pointer is not allowed");
        }
        setBytes(data.getBytes(StandardCharsets.UTF_8));

    }

    public static BytePointer unmanaged(Allocator allocator,long address, int capacity) {
        return new BytePointer(allocator,address,MemoryManager.sizeOfByte(),capacity,false,false);
    }

}
