package org.swdc.clang.libtooling.context;

import java.io.File;
import java.util.List;

public class MetaTypeUtils {

    public static final String FUNC_NAME_PLACEHOLDER = "=REPLACE_ME=";

    /**
     * 根据Type的CV修饰符生成C++的CV前缀。
     * @param type 元数据对象
     * @return C++的CV前缀。
     */
    public static String cppTypeModifier(AbstractMetaType type) {
        String modifier = "";
        if (type.isConst()) {
            modifier += "const ";
        }
        if (type.isVolatile()) {
            modifier += "volatile ";
        }
        return modifier;
    }

    /**
     * 根据指针类型链生成C++的多级指针修饰符字符串。
     * @param ptrType 指针元数据类型
     * @return C++指针修饰符字符串，格式为"* const* volatile ..."等
     */
    public static String cppPtrModifiers(PointerMetaType ptrType) {

        AbstractMetaType next = ptrType;
        StringBuilder castBuilder = new StringBuilder();
        while (next instanceof PointerMetaType) {
            PointerMetaType pointerMetaType = (PointerMetaType)next;
            castBuilder.append("* ").append(
                    MetaTypeUtils.cppTypeModifier(pointerMetaType)
            );
            next = pointerMetaType.getTargetType();
        }
        return castBuilder.toString();

    }

    /**
     * 根据函数元数据类型生成对应的C++函数签名字符串，函数名使用占位符替代。
     * @param type 函数元数据类型
     * @return C++函数签名，格式为"返回值类型 (占位符)(参数类型列表)"
     */
    public static String cppFunctionType(FunctionMetaType type) {

        List<AbstractMetaType> parameterTypes = type.getParameterTypes();
        StringBuilder paramBuilder = new StringBuilder();
        for (int index = 0; index < parameterTypes.size(); index++) {

            AbstractMetaType paramType = parameterTypes.get(index);
            String rawModifier = "";

            String typeName = paramType.getName();
            if (paramType instanceof PointerMetaType) {

                PointerMetaType pointerMetaType = (PointerMetaType)paramType;
                String paramTypeModifier = MetaTypeUtils.cppPtrModifiers(pointerMetaType);
                AbstractMetaType target = MetaTypeUtils.getLastPointee((PointerMetaType)paramType);
                typeName = target.getName() + paramTypeModifier;
                rawModifier = MetaTypeUtils.cppTypeModifier(target);

            } else {

                rawModifier = MetaTypeUtils.cppTypeModifier(paramType);

            }

            if (!rawModifier.endsWith(" ")) {
                rawModifier = rawModifier + " ";
            }

            paramBuilder.append(rawModifier).append(typeName);
            if (index < parameterTypes.size() - 1) {
                paramBuilder.append(", ");
            }

        }

        paramBuilder.insert(0, "(").append(")");

        AbstractMetaType returnType = type.getReturnType();
        String returnTypeModifier = MetaTypeUtils.cppTypeModifier(returnType);
        String returnTypeName = returnTypeModifier + returnType.cppTypeFullName();
        if (!returnTypeName.endsWith(" ")) {
            returnTypeName = returnTypeName + " ";
        }

        return returnTypeName + "(" + FUNC_NAME_PLACEHOLDER + ")" +  paramBuilder;
    }

    /**
     * 沿指针类型链逐级解引用，获取最终指向的非指针元数据类型。
     * @param ptrType 指针元数据类型
     * @return 指针链末端所指的元数据类型，若本身非指针则返回其自身
     */
    public static AbstractMetaType getLastPointee(PointerMetaType ptrType) {

        AbstractMetaType next = ptrType;
        while (next instanceof PointerMetaType) {
            PointerMetaType pointerMetaType = (PointerMetaType)next;
            next = pointerMetaType.getTargetType();
        }
        return next;

    }
    
    /**
     * 获取文件名中最后一个点号之前的部分，即去除扩展名后的主文件名。
     *
     * @param file 目标文件对象
     * @return 文件名中最后一个点号之前的部分；若文件名不含点号则返回完整文件名
     */
    public static String fileName(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex != -1) {
            return name.substring(0,dotIndex);
        }
        return name;
    }

}
