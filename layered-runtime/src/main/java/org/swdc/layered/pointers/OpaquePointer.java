package org.swdc.layered.pointers;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OpaquePointer {

    /**
     * 本指针是否具备所有权
     * 如果为true，则内存是被Allocator申请的，
     * 应该通过本指针或者Allocator最终释放。
     */
    private boolean owner = false;

    /**
     * 分配器
     */
    private Allocator allocator;

    /**
     * 内存地址。
     */
    private long address;

    /**
     * 对齐到多大的内存空间。
     */
    private int alignment;

    /**
     * 原始指针，本指针对象来自该指针，当本指针被释放时，
     * 最顶层（无source对象）的指针的引用计数将会减少。
     */
    private OpaquePointer source = null;

    /**
     * 锁，用于同步内存释放。
     */
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 创建一个新的指针实例。
     * <p>
     * 该构造函数用于直接初始化一个指针，通常由分配器调用以创建新的指针对象。
     *
     * @param allocator  内存分配器，用于管理内存的申请和释放
     * @param address    指向的内存地址
     * @param alignment  内存对齐要求
     * @param owner      是否具备内存所有权，为true时需负责释放内存
     */
    protected OpaquePointer(Allocator allocator, long address, int alignment, boolean owner) {
        this.address = address;
        this.alignment = alignment;
        this.owner = owner;
        this.allocator = allocator;
    }

    /**
     * 创建一个基于已有指针的偏移指针实例。
     * <p>
     * 该构造函数创建一个新的指针，指向源指针地址的指定偏移位置。
     * 新指针不具备所有权，但会增加源指针的引用计数。
     *
     * @param allocator  内存分配器
     * @param source     源指针，新指针基于此指针计算偏移
     * @param offset     相对于源指针地址的字节偏移量
     */
    protected OpaquePointer(Allocator allocator, OpaquePointer source, long offset) {

        this.source = source;
        this.address = source.getAddress() + offset;
        this.alignment = source.getAlignment();
        this.allocator = allocator;
        this.owner = false;

        allocator.reference(this);

    }

    public OpaquePointer(UniquePointer pointer) {

        this.address = pointer.getAddress();
        this.alignment = pointer.getAlignment();
        this.owner = pointer.isOwner();
        this.allocator = pointer.getAllocator();
        this.source = pointer.getSource();
        if (this.source != null) {
            allocator.reference(this);
        }

        pointer.moved(this);

    }

    public boolean isNull() {
        try {
            lock.readLock().lock();
            return address == 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getAlignment() {
        return alignment;
    }

    public long getAddress() {
        return address;
    }

    public boolean isOwner() {
        return owner;
    }

    public <T extends OpaquePointer> T offset(long offset) {
        try {
            lock.readLock().lock();
            OpaquePointer pointer = allocator.getRefManagedPointer(this.getAddress() +  offset);
            if (pointer != null) {
                return (T) pointer;
            }
            return (T) new OpaquePointer(this.allocator, this, offset);
        } finally {
            lock.readLock().unlock();
        }

    }


    /**
     * 强制清空当前指针的全部状态。
     * <p>
     * 该方法仅供分配器内部使用,清空地址、所有权、分配器、对齐以及来源均被置空。
     *
     * @throws IllegalStateException 当调用方不是 {@link Allocator} 时抛出
     */
    void unsafeClear() {
        try {
            lock.writeLock().lock();
            this.address = 0;
            this.owner = false;
            this.allocator = null;
            this.alignment = 0;
            this.source = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 释放当前指针所占用的内存资源。
     * <p>
     * 如果本指针来源于其他指针（source 不为 null），则减少顶层指针的引用计数；
     * 否则，如果本指针拥有所有权（owner 为 true），则通过分配器释放对应的内存。
     * 释放完成后，会清空地址、所有权、分配器、对齐以及来源等状态。
     */
    public void close() {
        try {

            this.lock.writeLock().lock();
            if (isNull()) {
                return;
            }

            if (source != null) {
                this.allocator.unReference(this);
                return;
            } else if (owner) {
                if (allocator.getReferenceCount(this) > 0) {
                    throw new IllegalStateException("There is still references to this pointer");
                }
                allocator.free(address, alignment);
            }
            unsafeClear();

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 将当前指针转换为具有独占所有权语义的 {@link UniquePointer}。
     * <p>
     * 该操作在写锁的保护下捕获当前指针的地址、对齐、所有权以及分配器，
     * 并据此构造一个独占指针。当该独占指针最终被释放时，若其具备所有权，
     * 则会校验引用计数并释放对应的内存空间。
     *
     * @return 基于当前指针构造的独占指针 {@link UniquePointer}
     * @throws IllegalStateException 当当前指针为空（地址为 0）时抛出
     */
    public <T extends OpaquePointer> UniquePointer<T> move() {

        if (isNull()) {
            throw new IllegalStateException("Pointer is null");
        }

        try {

            lock.writeLock().lock();

            long address = this.address;
            int alignment = this.alignment;
            boolean owner = this.owner;
            Allocator allocator = this.allocator;
            UniquePointer ptr = new UniquePointer<>(this.getClass(), allocator, this, () -> {

                if (owner) {

                    if (allocator.getReferenceCount(this) > 0) {
                        throw new IllegalStateException("There is still references to this pointer");
                    }
                    allocator.free(address, alignment);

                }

            });

            return (UniquePointer<T>)ptr;
        } finally {
            lock.writeLock().unlock();
        }

    }

    public OpaquePointer getSource() {
        return source;
    }

    protected Allocator getAllocator() {
        return allocator;
    }
}
