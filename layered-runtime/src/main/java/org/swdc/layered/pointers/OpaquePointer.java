package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

import java.lang.reflect.Constructor;

/**
 * 不透明指针，只有一些非常基础的属性，
 * 本指针只提供一种内存管理的形式，但是并不具体执行它。
 * 本指针可以被继承，用于各类本地对象的class。
 */
public class OpaquePointer {

    /**
     * 本指针是否具备所有权
     * 如果为true，则内存可以被本指针释放。
     */
    private boolean owner = false;

    /**
     * 本指针的引用计数，用于维持资源申请和释放的安全性。
     */
    private volatile int refCount = 0;

    /**
     * 源指针，用于偏移构造方法中，记录原始指针的引用。
     */
    private OpaquePointer source;

    /**
     * 分配器
     */
    private Allocator allocator;

    /**
     * 内存地址。
     */
    private long address;

    public long getAddress() {
        return address;
    }

    public boolean isNull() {
        return address == 0L;
    }

    public <T> T as(Class<T> clazz) {
        if (clazz.isInstance(this) || clazz.isAssignableFrom(this.getClass())) {
            return clazz.cast(this);
        }
        throw new ClassCastException(String.format("%s is not assignable from %s.", clazz.getName(), this.getClass().getName()));
    }


    public <T extends OpaquePointer> T reinterrupt(Class<T> clazz) {
        if (clazz.isInstance(this) || clazz.isAssignableFrom(this.getClass())) {
            return clazz.cast(this);
        }
        try {
            int capacity = 1;
            if (SeekablePointer.class.isAssignableFrom(clazz)) {
                // 移除界限，让它成为一个无界读写的指针。
                // 这种指针的读写是危险的，使用的时候自己想办法确认它的可用范围吧。
                capacity = 0;
            }
            Constructor<T> constructor = clazz.getDeclaredConstructor(
                    Allocator.class,long.class,int.class,int.class,boolean.class,boolean.class
            );
            constructor.setAccessible(true);
            T instance = constructor.newInstance(
                    getAllocator(),this.address, MemoryManager.sizeOfPointer(),capacity,false,this.isOwner()
            );
            getAllocator().castRef(instance);
            return instance;
        } catch (Exception e) {
        }

        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(
                    Allocator.class,long.class,boolean.class
            );
            constructor.setAccessible(true);
            T instance = constructor.newInstance(getAllocator(),this.address,this.isOwner());
            getAllocator().castRef(instance);
            return instance;
        } catch (Exception e) {
        }

        throw new ClassCastException(String.format("%s is not castable from %s.", clazz.getName(), this.getClass().getName()));
    }

    protected void initPointer(Allocator allocator, long address, boolean owner) {

        this.allocator = allocator;
        this.owner =  owner;
        this.address = address;
        this.allocator.ref(this);

    }

    protected void  initPointer(Allocator allocator, long address, OpaquePointer source) {

        this.allocator = allocator;
        this.owner = false;
        this.source = source;
        this.address = address;

        OpaquePointer target = source;
        if (target.getSource() != null) {
            target = source.getSource();
        }
        target.retain();
        allocator.ref(this);

    }

    public boolean isOwner() {
        return owner;
    }

    protected void deAllocate() {
    }

    /**
     * 增加引用计数，引用计数不为0的时候禁止释放内存。
     */
    protected final synchronized void retain() {
        refCount++;
    }

    /**
     * 减少引用计数，当引用计数为0的时候释放内存。
     */
    protected final synchronized void release() {
        refCount--;
    }

    /**
     * 释放内存，只有在引用计数为0的时候才能释放。
     * 如果本指针不具备所有权，则释放源指针的引用计数。
     */
    public synchronized void free() {

        if (isNull()) {
            return;
        }

        deAllocate();
        if (!owner) {
            allocator.unref(this);
            // 不是owner，只有通过本Runtime申请的内存的owner才能为true。
            if (source == null) {
                // 没有source
                return;
            }
            OpaquePointer target = source;
            while (target.source != null) {
                target = target.source;
            }
            target.release();
            source = null;
            address = 0;
            return;
        }

        if (refCount > 0) {
            throw new IllegalStateException("Cannot free a pointer with non-zero reference count");
        }

        address = 0L;
        owner = false;
    }

    protected OpaquePointer getSource() {
        return source;
    }

    public Allocator getAllocator() {
        return allocator;
    }


}
