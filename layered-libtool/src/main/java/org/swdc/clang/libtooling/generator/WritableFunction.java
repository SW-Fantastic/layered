package org.swdc.clang.libtooling.generator;

import org.swdc.clang.libtooling.context.MetaType;

import java.util.List;

/**
 * WritableFunction，代表了Wrapper中出现的本地函数的元数据，
 * 运行时需要加载这些元数据从而索引Wrapper的函数，实现本地API调用。
 */
public class WritableFunction {

    /**
     * Mangled name，函数的唯一标识符，由本项目通过libtooling提供的
     * Metadata，通过一定规则生成。
     * 本项目的运行时需要在代理对象中通过参数类型，函数名和返回值计算此字符串，
     * 用以获取functionIndex，从而获取函数的地址。
     */
    private String mangledName;

    /**
     * 参数类型列表，仅包含MetaType指定的几类基本类型，
     * Wrapper会负责把它们处理为合理的C++类型并执行函数调用。
     */
    private List<MetaType> params;

    /**
     * 返回值类型，仅包含MetaType指定的几类基本类型，
     * 本项目的运行时根需要据返回值的类型将基本类型进行包装，作为Java对象提供。
     */
    private MetaType returnType;

    /**
     * 函数索引，由本项目在生成wrapper的时候动态生成，
     * 运行时通过计算的MangledName读取此索引值，
     * 用以而获取本地函数的地址。
     */
    private int functionIndex;

    public WritableFunction(int index, String mangledName, List<MetaType> params, MetaType returnType) {
        this.functionIndex = index;
        this.mangledName = mangledName;
        this.params = params;
        this.returnType = returnType;
    }

    public String getMangledName() {
        return mangledName;
    }

    public MetaType getReturnType() {
        return returnType;
    }

    public List<MetaType> getParams() {
        return params;
    }

    public int getFunctionIndex() {
        return functionIndex;
    }
}
