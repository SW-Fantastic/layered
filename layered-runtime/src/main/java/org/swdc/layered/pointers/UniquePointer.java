package org.swdc.layered.pointers;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 独占指针，用于管理内存的唯一所有权。
 * @param <T>
 */
public final class UniquePointer<T> {

    private long address;

    private Allocator allocator;

    private int alignment;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private OpaquePointer source;

    private UniqueDestructor destructor;

    private boolean owner;

    private Class<T> sourceType;

    /**
     * 从已有指针接管所有权，构造独占指针。
     *
     * <p>该构造函数从指定的源指针接管内存所有权，包括地址、对齐方式、源指针链等信息。
     * 构造完成后，源指针的所有权转移至当前实例，其内部状态将被清空或引用计数减少。</p>
     *
     * @param sourceType 被接管源指针的类型，用于类型安全检查
     * @param allocator 负责管理该指针生命周期的内存分配器
     * @param source 被接管的源指针，构造后其所有权转移至当前实例
     * @param destructor 独占指针释放时调用的析构器，不能为空
     * @throws IllegalArgumentException 当 destructor 为 null 时抛出
     * @throws IllegalArgumentException 当 sourceType 为 null 时抛出
     * @throws IllegalArgumentException 当 source 为 null 或空指针时抛出
     */
    UniquePointer(Class<T> sourceType, Allocator allocator,OpaquePointer source, UniqueDestructor destructor) {

        if (destructor == null) {
            throw new IllegalArgumentException("destructor cannot be null");
        }

        if (sourceType == null) {
            throw new IllegalArgumentException("sourceType cannot be null");
        }

        if (source == null || source.isNull()) {
            throw new IllegalArgumentException("source cannot be null");
        }

        this.sourceType = sourceType;
        this.destructor = destructor;
        this.allocator = allocator;
        this.address = source.getAddress();
        this.alignment = source.getAlignment();
        this.source = source.getSource();
        this.owner = source.isOwner();
        
        this.allocator.pointerMove(source, this);

        if (source.getSource() != null) {
            this.allocator.unReference(source);
        } else {
            source.unsafeClear();
        }

    }

    public long getAddress() {
        return address;
    }

    public int getAlignment() {
        return alignment;
    }


    public Allocator getAllocator() {
        return allocator;
    }

    public OpaquePointer getSource() {
        return source;
    }

    public boolean isOwner() {
        return owner;
    }

    public Class<T> getSourceType() {
        return sourceType;
    }

    /**
     * 将当前独占指针标记为已移出（moved）状态。
     *
     * <p>当底层内存的所有权被转移给新的持有者时调用。若 {@code newOwner} 不为空，则先通过
     * {@code allocator.releaseMove(newOwner)} 通知分配器完成所有权转移；随后清空地址、分配器、
     * 对齐方式、源指针以及所有者标记等内部状态，使当前实例不再持有任何资源。整个过程持有写锁，
     * 保证与其它操作的线程安全。</p>
     *
     * @param newOwner 接收所有权的新持有者指针；为 null 时表示仅将当前实例标记为已移除，
     *                 不通知Allocator。
     */
    public void moved(OpaquePointer newOwner) {
        try {
            lock.writeLock().lock();

            if (newOwner != null) {
                allocator.releaseMove(newOwner);
            }

            this.address = 0;
            this.allocator = null;
            this.alignment = 0;
            this.source = null;
            this.owner = false;

        } finally {
            lock.writeLock().unlock();
        }
    }
    
   
    /**
     * 将此指针从内存管理中移除，移交其底层内存空间的所有权。
     *
     * <p>先取出当前地址并调用 {@code allocator.releaseMoveNative(this)} 解除分配器对该内存的
     * 跟踪，再通过 {@code moved(null)} 清空地址、分配器等内部状态，最后返回原始地址。当本地
     * 方法需要一个唯一指针作为输入、由本地代码接管内存生命周期时使用。整个过程持有写锁，
     * 保证与其它操作的线程安全。</p>
     *
     * 如果不确定本地函数是否会释放它，请不要随意使用本方法，否则可能导致内存泄漏。
     *
     * @return 该指针原先持有的底层内存地址
     */
    public long unmanaged() {
        try {

            lock.writeLock().lock();
            long address = this.address;
            allocator.releaseMoveNative(this);
            moved(null);
            return address;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 释放当前独占指针持有的资源，并将其重置为已移出状态。
     *
     * <p>先调用析构器释放底层内存，再通过 {@code moved(null)} 清空地址、分配器、
     * 对齐等内部状态；整个释放过程持有写锁，保证与其它操作的线程安全。</p>
     */
    public void close() {
        try {

            this.lock.writeLock().lock();
            this.destructor.free();
            moved(null);

        } finally {
            lock.writeLock().unlock();
        }
    }

}
