package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BytePointer extends SeekablePointer{

    protected BytePointer(Allocator allocator, long address, int alignment, boolean owner, int capacity) {
        super(allocator, address, alignment, owner);
        this.capacity = capacity;
    }

    protected BytePointer(Allocator allocator, OpaquePointer source, long offset) {
        super(allocator, source, offset);
    }

    public BytePointer(UniquePointer pointer) {
        super(pointer);
    }

    @Override
    public int getElementMemorySize() {
        return MemoryManager.sizeOfByte();
    }

    public byte get(int index) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        return MemoryManager.readByte(getAddress(), index);
    }

    public void set(int index, byte value) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        MemoryManager.writeByte(getAddress(), index, value);
    }

    public byte[] getArray(int offset, int size) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        return MemoryManager.readByteArray(getAddress(),offset, size);
    }

    public void setArray(int srcOffset, int destOffset, byte[] values) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        MemoryManager.writeByteArray(getAddress(),destOffset, values, srcOffset,values.length);
    }


    public String getAsString() {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        return getAsString(StandardCharsets.UTF_8);
    }

    public String getAsString(Charset charset) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (getCapacity() <= 0) {;
            throw new RuntimeException("Unknown byte length");
        }
        return getAsString(0, (int)getCapacity(),charset);
    }


    public String getAsString(int offset, int size, Charset charset) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        byte[] bytes = getArray(offset, size);
        return new String(bytes, charset);
    }

    public void setString(String value) {
        if(isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        setString(value, StandardCharsets.UTF_8);
    }

    public void setString( String value, Charset charset) {
        if(isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        setString(0, value, charset);
    }

    public void setString(int offset, String value, Charset charset) {
        if(isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        byte[] bytes = value.getBytes(charset);
        setArray(0, offset, bytes);
    }

}
