package org.swdc.clang.libtooling.context;

import java.util.ArrayList;
import java.util.List;

public class FunctionMetaType extends AbstractMetaType {

    private boolean callback;

    private AbstractMetaType returnType;

    private List<AbstractMetaType> parameterTypes = new ArrayList<>();

    public void setReturnType(AbstractMetaType returnType) {
        this.returnType = returnType;
    }

    public AbstractMetaType getReturnType() {
        return returnType;
    }

    public void setParameterTypes(List<AbstractMetaType> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public List<AbstractMetaType> getParameterTypes() {
        return parameterTypes;
    }

    public boolean isCallback() {
        return callback;
    }

    public void setCallback(boolean callback) {
        this.callback = callback;
    }

    /**
     * 生成函数的修饰名（Mangled Name）。
     * 格式为：(返回类型)命名空间::函数名@参数类型1,参数类型2,...
     * 用于在运行时唯一标识函数签名。
     *
     * @return 函数的修饰名字符串
     * @throws NullPointerException 当返回类型为 null 时抛出
     */
    @Override
    public String getMangledName() {

        if (returnType == null) {
            throw new NullPointerException("Return type is null");
        }

        if (parameterTypes == null) {
            parameterTypes = new ArrayList<>();
        }

        String name = "";
        if (!getNamespace().isBlank()){
            name = getNamespace() + "::"  + getName();
        } else {
            name = getName();
        }

        String returnTypeName = "(" + returnType.getMangledName() + ")";
        String paramTypeName = "";
        for (int index = 0; index < parameterTypes.size(); index++) {
            AbstractMetaType paramType = parameterTypes.get(index);
            String paramName = paramType.getMangledName();
            if (paramTypeName.isBlank()) {
                paramTypeName = paramName;
            } else {
                paramTypeName = paramTypeName + paramName;
            }
            if (index + 1 < parameterTypes.size()) {
                paramTypeName = paramTypeName + ",";
            }
        }

        return returnTypeName + name + "@" + paramTypeName;

    }


    /**
     * 生成从原始类型转换为本函数指针类型的C++语句。
     * 通过 reinterpret_cast 将原始类型指针 paramName 转换为本函数指针类型
     * 并赋值给 varName。
     *
     * @param varName 转换后使用的变量名
     * @param paramName 被转换的变量名，即原始类型的指针或地址
     * @return 类型转换语句，形如 "FuncType* varName = reinterpret_cast<FuncType*>(paramName);"
     * @throws IllegalArgumentException 当 varName 或 paramName 为 null 或空白时抛出
     */
    @Override
    public String castFromRawType(String varName, String paramName) {

        if (varName == null || varName.isBlank() || paramName == null || paramName.isBlank()) {
            throw new IllegalArgumentException("Cannot cast to a null or blank pointer type");
        }
        
        String targetType = MetaTypeUtils.cppFunctionType(this);
        return targetType.replace(MetaTypeUtils.FUNC_NAME_PLACEHOLDER, "* " + varName) +
                " = reinterpret_cast<" +
                targetType.replace(MetaTypeUtils.FUNC_NAME_PLACEHOLDER, "*") +
                ">(" + paramName + ");";
    }

    /**
     * 生成从本函数指针类型转换为基本类型（原始类型）的C++语句。
     * 通过 reinterpret_cast 将函数指针 paramName 转换为其对应的
     * 原始类型并赋值给 varName。
     *
     * @param varName 转换后使用的变量名
     * @param paramName 被转换的变量名，即本函数指针变量
     * @return 类型转换语句，形如 "RawType varName = reinterpret_cast<RawType>(paramName);"
     * @throws IllegalArgumentException 当 varName 或 paramName 为 null 或空白时抛出
     */
    @Override
    public String castToRawType(String varName, String paramName) {
        if (varName == null || varName.isBlank() || paramName == null || paramName.isBlank()) {
            throw new IllegalArgumentException("Cannot cast to a null or blank pointer type");
        }

        return getRawType().getTypeName() + " " + varName + " = reinterpret_cast<" + getRawType().getTypeName() + ">(" + paramName + ");";
    }

    /**
     * 获取函数的完整类型名，包括修饰符和指针。
     * @return 函数的完整类型名，例如 "void(*)(int, double)" 表示一个返回void的函数指针，
     */
    @Override
    public String cppTypeFullName() {
        return MetaTypeUtils.cppFunctionType(this).replace(
                MetaTypeUtils.FUNC_NAME_PLACEHOLDER, "*"
        );
    }

    @Override
    public String cppTypeFullName(String varName) {
        return MetaTypeUtils.cppFunctionType(this).replace(
                MetaTypeUtils.FUNC_NAME_PLACEHOLDER, "*" + varName
        );
    }
}
