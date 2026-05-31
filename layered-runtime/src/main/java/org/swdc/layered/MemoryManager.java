package org.swdc.layered;

import java.nio.ByteBuffer;

public class MemoryManager {

    public static native int sizeOfPointer();

    public static native int sizeOfInt();

    public static native int sizeOfLong();

    public static native int sizeOfDouble();

    public static native int sizeOfFloat();

    public static native int sizeOfByte();

    public static native int sizeOfShort();

    public static native int sizeOfBoolean();

    public static native long malloc(int size);

    public static native long mallocAligned(int size, int alignment);

    public static native int free(long address);

    public static native int freeAligned(long address);

    public static native boolean memset(long address, int value, long size);

    public static native boolean memcpy(long dest, long src, long size);

    public static native long strlen(long address);

    public static native ByteBuffer directBuffer(long address, long size);

    public static native int readInt(long address, int index);

    public static native int[] readIntArray(long address, int size);

    public static native void writeIntArray(long address, int[] values);

    public static native void writeInt(long address, int index, int value);

    public static native long readLong(long address, int index);

    public static native long[] readLongArray(long address, int size);

    public static native void writeLong(long address, int index, long value);

    public static native void writeLongArray(long address, long[] values);

    public static native double readDouble(long address, int index);

    public static native double[] readDoubleArray(long address, int size);

    public static native void writeDouble(long address, int index, double value);

    public static native void writeDoubleArray(long address, double[] values);

    public static native float readFloat(long address, int index);

    public static native float[] readFloatArray(long address, int size);

    public static native void writeFloat(long address, int index, float value);

    public static native void writeFloatArray(long address, float[] values);

    public static native short readShort(long address, int index);

    public static native short[] readShortArray(long address, int size);

    public static native void writeShort(long address, int index, short value);

    public static native void writeShortArray(long address, short[] values);

    public static native boolean readBoolean(long address, int index);

    public static native boolean[] readBooleanArray(long address, int size);

    public static native void writeBoolean(long address, int index, boolean value);

    public static native void writeBooleanArray(long address, boolean[] values);

    public static native long readAddress(long address, int index);

    public static native long[] readAddressArray(long address, int size);

    public static native void writeAddress(long address, int index, long value);

    public static native void writeAddressArray(long address, long[] values);

    public static native byte readByte(long address, int index);

    public static native byte[] readByteArray(long address, int offset, int size);

    public static native void writeByte(long address, int index, byte value);

    public static native void writeByteArray(long address, int dstOffset, byte[] values, int srcOffset, int size);


}
