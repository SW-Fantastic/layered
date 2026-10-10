package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 内存分配器，负责管理指针的创建、引用计数和释放。
 */
public class Allocator {

    /**
     * 存储已分配的指针，用于跟踪和释放内存。
     */
    private Map<Long, OpaquePointer> allocatedPointers = new ConcurrentHashMap<>();

    /**
     * 存储引用的二级指针，用于追踪和释放指针引用。
     */
    private Map<Long, Map<Long, OpaquePointer>> referencedPointers = new ConcurrentHashMap<>();

    /**
     * 存储借用指针，用于追踪借用指针的实例。
     */
    private Map<Long, OpaquePointer> borrowedPointers = new ConcurrentHashMap<>();

    /**
     * 存储移动指针，方便释放执行了move但是尚未被使用的游离指针。
     */
    private Map<Long, UniquePointer> movingPointers = new ConcurrentHashMap<>();


    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 获取指定指针的引用计数。
     * <p>
     * 通过指针地址查找引用关系表，返回引用该地址的指针数量；若不存在引用关系则返回 0。
     *
     * @param pointer 待查询的指针
     * @return 该指针的引用计数，无引用时返回 0
     */
    public int getReferenceCount(OpaquePointer pointer) {

        try {
            lock.readLock().lock();
            OpaquePointer ptr = PtrUtils.getRawPointer(pointer);
            Long address = ptr.getAddress();
            Map<Long, OpaquePointer> map = referencedPointers.get(address);
            if (map == null) {
                return 0;
            }
            return map.size();
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * 记录指定指针的引用关系。
     *
     * <p>若该指针没有源指针（{@link OpaquePointer#getSource()} 返回 {@code null}），
     * 则将其作为借用指针登记到借用指针映射中；否则沿源指针链回溯至原始指针，
     * 并在以原始指针地址为键的引用映射中登记该指针，用于追踪二级指针，
     * 以便在释放内存时一并关闭。</p>
     *
     * @param pointer 待登记的指针
     */
    void reference(OpaquePointer pointer) {

        // 始终追踪原始指针。
        try {
            lock.writeLock().lock();
            if (pointer.getSource() == null) {
                borrowedPointers.put(pointer.getAddress(), pointer);
                return;
            }

            OpaquePointer raw = PtrUtils.getRawPointer(pointer);
            Long address = raw.getAddress();
            Map<Long, OpaquePointer> map = referencedPointers.get(address);
            if (map == null) {
                map = new ConcurrentHashMap<>();
                referencedPointers.put(address, map);
            }
            if (map.containsKey(pointer.getAddress())) {
                throw new RuntimeException("Address already referenced !");
            }
            map.put(pointer.getAddress(), pointer);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 解除指定指针的引用关系。
     *
     * <p>若该指针没有源指针（{@link OpaquePointer#getSource()} 返回 {@code null}），
     * 则将其从借用指针映射中移除；否则沿源指针链回溯至原始指针，从以原始指针地址
     * 为键的引用映射中移除该指针，并清空该指针以避免其被重复释放。</p>
     *
     * @param pointer 待解除引用的指针
     */
    void unReference(OpaquePointer pointer) {

        try {
            lock.writeLock().lock();
            if (pointer.getSource() == null) {
                borrowedPointers.remove(pointer.getAddress());
                return;
            }

            OpaquePointer raw = PtrUtils.getRawPointer(pointer);
            Map<Long, OpaquePointer> map = referencedPointers.get(raw.getAddress());
            if (map != null) {
                long address = pointer.getAddress();
                pointer.unsafeClear();
                map.remove(address);
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * 记录指针的移动操作，将源指针交由移动后的唯一指针接管。
     *
     * <p>若源指针并非所有者，则不做任何处理直接返回；否则从已分配指针集合中
     * 移除源指针，并将移动后的指针登记到移动指针集合中，以便在释放执行了 move
     * 但尚未被使用的游离指针时一并释放。</p>
     *
     * @param source 被移动的源指针，需为所有者才会执行移动记录
     * @param moved  移动后产生的唯一指针，将被登记到移动指针集合中
     */
    void pointerMove(OpaquePointer source, UniquePointer moved) {
        try {

            lock.writeLock().lock();
            if (!source.isOwner()) {
                return;
            }
            allocatedPointers.remove(source.getAddress());
            movingPointers.put(moved.getAddress(), moved);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 释放指针的移动状态。
     *
     * <p>将指针从移动中状态移除，并恢复其正常的引用管理。如果指针是所有者，
     * 则将其添加到已分配指针集合；否则建立源指针到目标指针的引用关系。</p>
     *
     * @param target 待释放移动状态的指针
     */
    void releaseMove(OpaquePointer target) {

        try {
            lock.writeLock().lock();
            UniquePointer ptr = movingPointers.remove(target.getAddress());
            if (ptr != null) {
                if (ptr.isOwner()) {
                    allocatedPointers.put(ptr.getAddress(), target);
                } else {
                    reference(target);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

    }
    
    /**
     * 释放唯一指针，在不释放内存的情况下释放其移动状态，
     * 使此指针脱管，如果唯一指针需要在本地函数中释放，
     * 则需要调用此方法。该方法不会释放内存，只是将指针从移动状态中移除。
     *
     * @param ptr 待释放移动状态的唯一指针
     */
    void releaseMoveNative(UniquePointer ptr) {
        try {
            lock.writeLock().lock();
            movingPointers.remove(ptr.getAddress());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 分配指定容量的整型内存空间，清零后返回对应的整型指针。
     *
     * @param capacity 需要分配的整型元素个数，必须为正数
     * @return 指向已分配并清零的整型内存的指针
     * @throws IllegalArgumentException 当 capacity 小于等于 0 时抛出
     * @throws RuntimeException 当底层内存分配失败时抛出
     */
    public IntPointer allocateInt(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal capacity: " + capacity);
        }

        int memorySize = MemoryManager.sizeOfInt() * capacity;
        long address = MemoryManager.malloc(memorySize);
        if (address == 0) {
            throw new RuntimeException("Failed to allocate memory");
        }

        MemoryManager.memset(address, 0, memorySize);
        IntPointer pointer = new IntPointer(this, address, -1, true, capacity);
        allocatedPointers.put(address, pointer);
        return pointer;

    }

    /**
     * 分配指定容量的长整型数组内存。
     * <p>
     * 该方法在本地内存中分配足够存储指定数量长整型值的空间，
     * 并将所有字节初始化为零。返回的LongPointer可用于读写该内存区域。
     *
     * @param capacity 要分配的长整型元素数量，必须大于0
     * @return 指向已分配内存的LongPointer实例
     * @throws IllegalArgumentException 如果capacity小于等于0
     * @throws RuntimeException 如果内存分配失败
     */
    public LongPointer allocateLong(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal capacity: " + capacity);
        }
        int memorySize = MemoryManager.sizeOfLong() * capacity;
        long address = MemoryManager.malloc(memorySize);
        if (address == 0) {
            throw new RuntimeException("Failed to allocate memory");
        }
        MemoryManager.memset(address, 0, memorySize);
        LongPointer pointer = new LongPointer(this, address, -1, true, capacity);
        allocatedPointers.put(address, pointer);
        return pointer;
    }
    
    /**
     * 分配指定容量的浮点数内存，并返回对应的FloatPointer。
     *
     * @param capacity 要分配的浮点数元素数量，必须大于0
     * @return 新分配的FloatPointer，指向已初始化为零的内存
     * @throws IllegalArgumentException 如果capacity小于等于0
     * @throws RuntimeException 如果内存分配失败
     */
    public FloatPointer allocateFloat(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal capacity: " + capacity);
        }
        int memorySize = MemoryManager.sizeOfFloat() * capacity;
        long address = MemoryManager.malloc(memorySize);
        if (address == 0) {
            throw new RuntimeException("Failed to allocate memory");
        }
        MemoryManager.memset(address, 0, capacity);
        FloatPointer pointer = new FloatPointer(this, address, -1, true, capacity);
        allocatedPointers.put(address, pointer);
        return pointer;
    }
    
    
    /**
     * 分配指定容量的双精度浮点数内存，并返回对应的DoublePointer。
     *
     * @param capacity 要分配的双精度浮点数元素数量，必须大于0
     * @return 新分配的DoublePointer，指向已初始化为零的内存
     * @throws IllegalArgumentException 如果capacity小于等于0
     * @throws RuntimeException 如果内存分配失败
     */
    public DoublePointer allocateDouble(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal capacity: " + capacity);
        }
        int memorySize = MemoryManager.sizeOfDouble() * capacity;
        long address = MemoryManager.malloc(memorySize);
        if (address == 0) {
            throw new RuntimeException("Failed to allocate memory");
        }
        MemoryManager.memset(address, 0, capacity);
        DoublePointer pointer = new DoublePointer(this, address, -1, true, capacity);
        allocatedPointers.put(address, pointer);
        return pointer;
    }

    public ShortPointer allocateShort(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal capacity: " + capacity);
        }
        int memorySize = MemoryManager.sizeOfShort() * capacity;
        long address = MemoryManager.malloc(memorySize);
        if (address == 0) {
            throw new RuntimeException("Failed to allocate memory");
        }
        MemoryManager.memset(address, 0, capacity);
        ShortPointer pointer = new ShortPointer(this, address, -1, true, capacity);
        allocatedPointers.put(address, pointer);
        return pointer;
    }

    
    /**
     * 分配指定容量的指针数组内存，并返回对应的二级指针。
     *
     * <p>在本地内存中分配可容纳 {@code capacity} 个指针的空间，
     * 分配后将内存清零，并记录到本分配器管理的指针集合中。
     * 返回的PtrPointer可通过泛型参数T指定元素所引用的指针类型。</p>
     *
     * @param capacity 要分配的指针元素数量，必须大于0
     * @return 指向已分配并清零内存的PtrPointer实例
     * @throws IllegalArgumentException 如果capacity小于等于0
     * @throws RuntimeException 如果内存分配失败
     */
    public <T extends OpaquePointer> PtrPointer<T> allocatePtr(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal capacity: " + capacity);
        }
        int memorySize = MemoryManager.sizeOfPointer() * capacity;
        long address = MemoryManager.malloc(memorySize);
        if (address == 0) {
            throw new RuntimeException("Failed to allocate memory");
        }
        MemoryManager.memset(address, 0, capacity);
        PtrPointer<T> pointer = new PtrPointer<>(this, address, -1, true, capacity);
        allocatedPointers.put(address, pointer);
        return pointer;
    }
    
    /**
     * 分配指定容量的字节内存。
     *
     * @param capacity 要分配的字节容量，必须大于0
     * @return 分配成功的BytePointer实例
     * @throws IllegalArgumentException 如果capacity小于等于0
     * @throws RuntimeException 如果内存分配失败
     */
    public BytePointer allocateByte(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal capacity: " + capacity);
        }
        int memorySize = MemoryManager.sizeOfByte() * capacity;
        long address = MemoryManager.malloc(memorySize);
        if (address == 0) {
            throw new RuntimeException("Failed to allocate memory");
        }
        MemoryManager.memset(address, 0, capacity);
        BytePointer pointer = new BytePointer(this, address, -1, true, capacity);
        allocatedPointers.put(address, pointer);
        return pointer;
    }


    /**
     * 分配按指定字节数对齐的字节内存。
     *
     * <p>按 {@code alignment} 指定的对齐字节数分配 {@code capacity} 个字节的内存，
     * 分配后将内存清零，并记录到本分配器管理的指针集合中。</p>
     *
     * @param capacity  要分配的字节容量，必须大于0
     * @param alignment 内存对齐字节数，必须大于0
     * @return 分配成功的BytePointer实例
     * @throws IllegalArgumentException 如果capacity或alignment小于等于0
     * @throws RuntimeException         如果对齐内存分配失败
     */
    public BytePointer allocateByteAligned(int capacity, int alignment) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Illegal capacity: " + capacity);
        }
        if (alignment <= 0) {
            throw new IllegalArgumentException("Illegal alignment: " + alignment);
        }
        int memorySize = MemoryManager.sizeOfByte() * capacity;
        long address = MemoryManager.mallocAligned(memorySize, alignment);
        if (address == 0) {
            throw new RuntimeException("Failed to allocate aligned memory");
        }
        MemoryManager.memset(address, 0, capacity);
        BytePointer pointer = new BytePointer(this, address, -1, true, capacity);
        allocatedPointers.put(address, pointer);
        return pointer;
    }

    /**
     * 释放指定地址的内存。
     *
     * <p>若 {@code address} 为 0，则直接返回，不做任何处理；
     * 当 {@code alignment} 小于等于 0 时，按普通方式释放内存，
     * 否则按指定的对齐字节数释放对齐内存。</p>
     *
     * @param address   要释放的内存地址，为 0 时直接返回
     * @param alignment 内存对齐字节数，小于等于 0 时表示按普通内存释放
     */
    public void free(long address, int alignment) {

        if (address == 0) {
            return;
        }
        if (alignment <= 0) {
            MemoryManager.free(address);
        } else {
            MemoryManager.freeAligned(address, alignment);
        }

    }

    public OpaquePointer getRefManagedPointer(long address) {
        try {
            lock.readLock().lock();

            if (borrowedPointers.containsKey(address)) {
                return borrowedPointers.get(address);
            }

            for (Map<Long, OpaquePointer> refs : referencedPointers.values()) {
                if (refs.containsKey(address)) {
                    return refs.get(address);
                }
            }

            return null;

        } finally {
            lock.readLock().unlock();
        }
    }

    public OpaquePointer getAnyManagedPointer(long address) {

        try {
            lock.readLock().lock();

            if(allocatedPointers.containsKey(address)){
                return allocatedPointers.get(address);
            }

            if (borrowedPointers.containsKey(address)) {
                return borrowedPointers.get(address);
            }

            if (movingPointers.containsKey(address)) {
                return new OpaquePointer(movingPointers.get(address));
            }

            for (Map<Long, OpaquePointer> refs : referencedPointers.values()) {
                if (refs.containsKey(address)) {
                    return refs.get(address);
                }
            }

            return null;

        } finally {
            lock.readLock().unlock();
        }

    }


    /**
     * 关闭当前分配器并释放其管理的全部内存。
     *
     * <p>依次释放所有借用的指针、本分配器分配的指针，
     * 以及这些指针关联的二级指针，最后清空各项指针记录。
     * 该方法为同步方法，确保释放过程线程安全。</p>
     */
    public synchronized void free() {

        try {

            lock.writeLock().lock();
            // 安全释放所有分配的指针
            for (OpaquePointer entry : allocatedPointers.values()) {
                // 释放所有本Allocator分配的内存
                if (referencedPointers.containsKey(entry.getAddress())) {
                    // 释放所有二级指针
                    Map<Long, OpaquePointer> referenced = referencedPointers.get(entry.getAddress());
                    for (OpaquePointer pointer : referenced.values()) {
                        pointer.close();
                    }
                    referenced.clear();
                    referencedPointers.remove(entry.getAddress());
                }
                entry.close();
            }
            allocatedPointers.clear();
            referencedPointers.clear();

            for (UniquePointer pointer : movingPointers.values()) {
                pointer.close();
            }
            movingPointers.clear();
            borrowedPointers.clear();

        } finally {
            lock.writeLock().unlock();
        }

    }

}
