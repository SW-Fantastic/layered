package org.swdc.clang.libtooling.context;

import java.util.List;

public class PointerMetaType extends AbstractMetaType {

    private AbstractMetaType targetType;

    @Override
    public MetaType getRawType() {
        return MetaType.INTPTR_T;
    }

    public void setTargetType(AbstractMetaType targetType) {
        this.targetType = targetType;
    }

    public AbstractMetaType getTargetType() {
        return targetType;
    }

    @Override
    public String getMangledName() {
        return MetaTypeUtils.cppPtrMangled(this);
    }


    @Override
    public String cppTypeFullName() {

        AbstractMetaType next = MetaTypeUtils.getLastPointee(this);
        String ptrModifier = MetaTypeUtils.cppPtrModifiers(this);
        if (next instanceof FunctionMetaType) {
            // 这个是函数指针，比较特殊。
            FunctionMetaType functionMetaType = (FunctionMetaType)next;
            String targetType = MetaTypeUtils.cppFunctionType(functionMetaType);
            return targetType.replace(MetaTypeUtils.FUNC_NAME_PLACEHOLDER, ptrModifier);
        }
        return  next.cppTypeFullName() + ptrModifier;

    }

    @Override
    public String cppTypeFullName(String varName) {

        AbstractMetaType metaType = MetaTypeUtils.getLastPointee(this);
        String ptrModifier = MetaTypeUtils.cppPtrModifiers(this);
        if (metaType instanceof FunctionMetaType) {
            FunctionMetaType functionMetaType = (FunctionMetaType)metaType;
            String targetType = MetaTypeUtils.cppFunctionType(functionMetaType);
            return targetType.replace(MetaTypeUtils.FUNC_NAME_PLACEHOLDER, ptrModifier + varName);
        }

        return cppTypeFullName() + " " + varName;

    }

    @Override
    public String castFromRawType(String varName, String paramName) {

        String ptrModifier = MetaTypeUtils.cppPtrModifiers(this);
        AbstractMetaType next = MetaTypeUtils.getLastPointee(this);

        if (next instanceof FunctionMetaType) {

            // 这个是函数指针，比较特殊。
            FunctionMetaType functionMetaType = (FunctionMetaType)next;
            String targetType = MetaTypeUtils.cppFunctionType(functionMetaType);
            return targetType.replace(MetaTypeUtils.FUNC_NAME_PLACEHOLDER, ptrModifier + varName) +
                    " = reinterpret_cast<" +
                    targetType.replace(MetaTypeUtils.FUNC_NAME_PLACEHOLDER, ptrModifier) +
                    ">(" + paramName + ");";

        } else {
            // getName提供libtooling解析出来的内容，包含原始类型名
            String targetType = next.cppTypeFullName() + ptrModifier;
            return targetType + " " + varName + " = reinterpret_cast<" + targetType + ">(" + paramName + ");";
        }

    }

    @Override
    public String castToRawType(String varName, String paramName) {

        if (varName == null || varName.isBlank() || paramName == null || paramName.isBlank()) {
            throw new IllegalArgumentException("Cannot cast to a null or blank pointer type");
        }

        return getRawType().getTypeName() + " " + varName + " = reinterpret_cast<" + getRawType().getTypeName() + ">(" + paramName + ");";

    }
}
