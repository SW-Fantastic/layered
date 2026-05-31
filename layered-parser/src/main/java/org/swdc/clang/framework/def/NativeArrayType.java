package org.swdc.clang.framework.def;

import org.swdc.layered.def.NativeDefinition;

public class NativeArrayType extends AbstractNativeType {

    private AbstractNativeType elementType;

    private long arraySize;

    public NativeArrayType(AbstractNativeType elementType) {
        super(null, NativeDefinition.ARRAY);
        this.elementType = elementType;
    }

    public AbstractNativeType getElementType() {
        return elementType;
    }

    public long getArraySize() {
        return arraySize;
    }

    public void setArraySize(long arraySize) {
        this.arraySize = arraySize;
    }

    @Override
    public String getName() {
        return "_Arr" + arraySize + "_" + elementType.getName();
    }

    @Override
    public NameCaster castAsBaseType() {
        return (name) -> {
            if (name == null || name.isEmpty()) {
                return "intptr_t";
            }
            return "intptr_t " + name;
        };
    }


    @Override
    public NameCaster castFromBaseType() {

        /*
        // 假设元数据告诉你这是一个 long[10][20]
        long* raw_ptr = ...; // API 传回来的原始指针

        // 1. 强转为“指向完整二维数组”的指针
        auto full_ptr = reinterpret_cast<long (*)[10][20]>(raw_ptr);

        // 2. 解引用，找回失踪的第一维度
        auto& original_array = *full_ptr;

        // 验证：
        static_assert(sizeof(original_array) == 10 * 20 * sizeof(long), "维度完美还原！");
         */

        return (name) -> {

            String arr = "[" + getArraySize() + "]";
            NativeArrayType arrayType = this;
            while (arrayType.elementType instanceof NativeArrayType) {
                arrayType = (NativeArrayType) arrayType.elementType;
                arr = arr + "[" + arrayType.getArraySize() + "]";
            }

            AbstractNativeType element = arrayType.elementType;

            if (name == null || name.isEmpty()) {
                return element.castFromBaseType().as("") + "(*)" + arr;
            }
            return element.castFromBaseType().as("") + " (*" + name + ")" + arr;

        };

    }

    public NameCaster castFromBaseTypeRef() {
        return (name) -> {

            String arr = "[" + getArraySize() + "]";
            NativeArrayType arrayType = this;
            while (arrayType.elementType instanceof NativeArrayType) {
                arrayType = (NativeArrayType) arrayType.elementType;
                arr = arr + "[" + arrayType.getArraySize() + "]";
            }

            AbstractNativeType element = arrayType.elementType;

            if (name == null || name.isEmpty()) {
                return element.castFromBaseType().as("") + "(&)" + arr;
            }
            return element.castFromBaseType().as("") + " (&" + name + ")" + arr;

        };
    }

    @Override
    public <T extends AbstractNativeType> T copy() {

        NativeArrayType array = new NativeArrayType(elementType);
        array.setArraySize(getArraySize());
        array.setConstType(isConstType());
        array.setVolatileType(isVolatileType());

        return (T)array;
    }
}
