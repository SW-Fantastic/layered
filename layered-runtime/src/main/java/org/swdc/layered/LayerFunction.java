package org.swdc.layered;

import java.util.List;

public class LayerFunction {

    /**
     * 目标函数名，在生成的Wrapper中，此名称代表了实际的本地函数的名称，
     * 运行时通过查找此名称获取函数地址。
     */
    private String targetName;

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


    public String getMangledName() {
        return mangledName;
    }

    public void setMangledName(String mangledName) {
        this.mangledName = mangledName;
    }

    public MetaType getReturnType() {
        return returnType;
    }

    public void setReturnType(MetaType returnType) {
        this.returnType = returnType;
    }

    public List<MetaType> getParams() {
        return params;
    }

    public void setParams(List<MetaType> params) {
        this.params = params;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }
}
