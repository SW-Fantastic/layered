package org.swdc.layered.calling;

import org.swdc.layered.MemoryManager;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.pointers.BytePointer;
import org.swdc.layered.pointers.OpaquePointer;
import org.swdc.layered.pointers.PtrPointer;

import java.util.concurrent.locks.ReentrantLock;

public class LayerBuffer {

    private BytePointer paramBuffer;

    private BytePointer resultBuffer;

    private PtrPointer paramPtr;

    private int paramCount;

    public LayerBuffer(Allocator allocator, int parameterCount) {

        if (parameterCount == 0) {
            parameterCount = 1;
        }

        int size = MemoryManager.getSizeOfPointer() * parameterCount;

        this.paramCount = parameterCount;
        this.paramPtr = allocator.allocatePtr(size);
        this.paramBuffer = allocator.allocateByte(size);
        this.resultBuffer = allocator.allocateByte(MemoryManager.getSizeOfPointer());

        MemoryManager.memset(paramPtr.getAddress(), 0, size);
        for (int i = 0; i < parameterCount; i++) {
            paramPtr.set(i, paramBuffer.offset(i * MemoryManager.getSizeOfPointer()));
        }
        reset();

    }

    public void reset() {

        if (paramBuffer == null || paramPtr == null || resultBuffer == null) {
            return;
        }

        if (paramBuffer.isNull() || paramPtr.isNull() || resultBuffer.isNull()) {
            return;
        }

        int size = MemoryManager.getSizeOfPointer() * paramCount;
        MemoryManager.memset(paramBuffer.getAddress(), 0, size);
        MemoryManager.memset(resultBuffer.getAddress(), 0, MemoryManager.getSizeOfPointer());

    }

    public OpaquePointer getParameterBuf(int index) {
        if (paramBuffer == null || paramBuffer.isNull()) {
            throw new RuntimeException("Buffer has closed.");
        }
        if (index < 0 || index >= paramCount) {
            throw new RuntimeException("Index out of bounds.");
        }
        return paramBuffer.offset(index * MemoryManager.getSizeOfPointer());
    }

    public OpaquePointer getResultBuf() {
        if (resultBuffer == null || resultBuffer.isNull()) {
            throw new RuntimeException("Buffer has closed.");
        }
        return resultBuffer;
    }

    public OpaquePointer getParamPtr() {
        if (paramPtr == null || paramPtr.isNull()) {
            throw new RuntimeException("Buffer has closed.");
        }
        return paramPtr;
    }

    public void close() {

        for (int i = 0; i < paramCount; i++) {
            OpaquePointer ptr = paramBuffer.offset(i * MemoryManager.getSizeOfPointer());
            ptr.close();
        }
        paramBuffer.close();
        resultBuffer.close();
        paramPtr.close();

        paramBuffer = null;
        resultBuffer = null;
        paramPtr = null;

    }

}
