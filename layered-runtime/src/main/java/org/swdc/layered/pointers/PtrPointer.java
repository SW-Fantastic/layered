package org.swdc.layered.pointers;

import org.swdc.layered.MemoryManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class PtrPointer<T extends OpaquePointer> extends SeekablePointer {

    private Class<T> type;

    protected PtrPointer(Allocator allocator, long address, int alignment, boolean owner) {
        super(allocator, address, alignment, owner);

    }

    protected PtrPointer(Allocator allocator, long address, int alignment, boolean owner, int capacity) {
        super(allocator, address, alignment, owner, capacity);
    }

    protected PtrPointer(Allocator allocator, OpaquePointer source, long offset) {
        super(allocator, source, offset);
    }

    public PtrPointer(UniquePointer pointer) {
        super(pointer);
    }

    public void set(int index, OpaquePointer address) {

        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        if (address.isNull()) {
            throw new RuntimeException("provided pointer is null");
        }

        long addressValue = address.getAddress();
        MemoryManager.writeAddress(getAddress(), index, addressValue);

    }

    public void setAddress(int index, Long address) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        MemoryManager.writeAddress(getAddress(), index, address);
    }

    /**
     * 读取指定索引处保存的地址，并转换为指定类型的指针。
     * <p>若该地址已有被管理的指针则复用并校验类型；否则创建新的不透明指针
     * 并注册到分配器，再按传入类型完成转换。</p>
     *
     * @param index 元素在指针数组中的索引
     * @param type  期望的指针类型，为 null 时直接返回不透明指针
     * @param <R>   返回指针的类型，须为 OpaquePointer 的子类
     * @return 指定索引处地址对应的指针
     * @throws RuntimeException 当前指针为空、已有指针类型不匹配或类型转换失败时抛出
     */
    public <R extends OpaquePointer> R getAddress(int index, Class<R> type) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        long address = MemoryManager.readAddress(getAddress(),index);
        OpaquePointer pointer = getAllocator().getAnyManagedPointer(address);
        if (pointer != null) {

            // 检验已存在的指针是否符合类型要求
            if (pointer.getClass().isAssignableFrom(type)) {
                return (R)pointer;
            }
            throw new RuntimeException("pointer type mismatch, pointer should be " + pointer.getClass().getName());

        }
        pointer = new OpaquePointer(getAllocator(), address, -1, false);
        getAllocator().reference(pointer);
        if (type != null) {
            if (OpaquePointer.class == type) {
                return (R)pointer;
            }
            try {
                if (pointer.isNull()) {
                    return null;
                }
                // 尝试通过构造函数转换指针类型
                Constructor<R> castCtor = type.getConstructor(UniquePointer.class);
                castCtor.setAccessible(true);
                return castCtor.newInstance(pointer.move());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            // 没有提供泛型类型，因此直接返回不透明指针本身。
            return (R) pointer;
        }
    }

    public T getAddress(int index) {
        if (isNull()) {
            throw new RuntimeException("Pointer is null");
        }
        return getAddress(index, getType());
    }

    /**
     * 通过反射机制从泛型字段中获取运行时类型信息。
     *
     * <p>该方法首先检查缓存的type是否已存在，若存在则直接返回。
     * 否则通过反射获取当前类的type字段，并尝试将其作为ParameterizedType
     * 解析以提取泛型类型参数。</p>
     *
     * @return type字段声明的泛型类型Class对象
     * @throws RuntimeException 如果type字段不存在
     */
    protected Class<T> getType() {
        if (type != null) {
            return type;
        }
        try {
            Field field = getClass().getDeclaredField("type");
            Type fieldGenericType = field.getGenericType();
            if (fieldGenericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) fieldGenericType;
                type = (Class<T>) parameterizedType.getActualTypeArguments()[0];
            }
            return type;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取单个元素（指针）占用的内存大小。
     *
     * @return 指针大小（字节）
     */
    @Override
    public int getElementMemorySize() {
        return MemoryManager.sizeOfPointer();
    }

}
