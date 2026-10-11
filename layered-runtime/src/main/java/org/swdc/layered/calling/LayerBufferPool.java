package org.swdc.layered.calling;

import org.swdc.layered.pointers.Allocator;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class LayerBufferPool {

    private final ConcurrentLinkedQueue<LayerBuffer> buffers = new ConcurrentLinkedQueue<>();

    private ReentrantLock lock = new ReentrantLock();

    private final int coreSize;

    private final int maxSize;

    private final int paramCount;

    private Allocator allocator;

    public LayerBufferPool(Allocator allocator, int paramCount, int coreSize, int maxSize) {

        this.coreSize = coreSize;
        this.maxSize = maxSize;
        this.paramCount = paramCount;
        this.allocator = allocator;

    }

    /**
     * 从缓冲池中借出一个 LayerBuffer。
     *
     * 若池中存在空闲缓冲，则出队并复用；否则通过分配器新建一个。
     * 该方法通过可重入锁保证线程安全，缓冲池关闭后将无法再借出。
     *
     * @return 可用的 LayerBuffer 实例
     * @throws IllegalStateException 当缓冲池已关闭（分配器为空）时抛出
     */
    public LayerBuffer borrow() {
       try {
           lock.lock();
           if (allocator == null) {
               throw new IllegalStateException("buffer pool has closed.");
           }
           if (buffers.isEmpty()) {
               return new LayerBuffer(allocator, paramCount);
           }
           return buffers.poll();
       } finally {
           lock.unlock();
       }
    }

    /**
     * 将 LayerBuffer 归还到缓冲池中。
     *
     * 若归还后池内缓冲数量未达到上限（maxSize），则将其入队以便后续复用；
     * 否则直接关闭该缓冲并释放其持有的底层资源。
     * 该方法通过可重入锁保证线程安全，缓冲池关闭后将无法归还。
     *
     * @param buffer 待归还的 LayerBuffer 实例
     * @throws IllegalStateException 当缓冲池已关闭（分配器为空）时抛出
     */
    public void release(LayerBuffer buffer) {
        try {
            lock.lock();
            if (allocator == null) {
                throw new IllegalStateException("buffer pool has closed.");
            }
            if (buffers.size() + 1 < maxSize) {
                buffers.offer(buffer);
            } else {
                buffer.close();
            }
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        try {
            lock.lock();
            while (!buffers.isEmpty()) {
                buffers.poll().close();
            }
            buffers.clear();
            allocator = null;
        } finally {
            lock.unlock();
        }
    }

}
