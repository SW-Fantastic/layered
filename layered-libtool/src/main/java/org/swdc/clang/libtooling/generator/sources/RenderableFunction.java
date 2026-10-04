package org.swdc.clang.libtooling.generator.sources;

import org.swdc.clang.libtooling.context.AbstractMetaType;
import org.swdc.clang.libtooling.context.FunctionMetaType;
import org.swdc.clang.libtooling.context.MetaType;

import java.util.List;

public class RenderableFunction {

    private FunctionMetaType functionMetaType;

    private String prefix;

    private int index;

    public RenderableFunction(String prefix, int index, FunctionMetaType functionMetaType) {
        this.index = index;
        this.prefix = prefix;
        this.functionMetaType = functionMetaType;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return functionMetaType.getName();
    }

    public AbstractMetaType getReturnType() {
        return functionMetaType.getReturnType();
    }

    public List<AbstractMetaType> getParamTypes() {
        return functionMetaType.getParameterTypes();
    }

    public boolean needResult() {
        return getReturnType().getRawType() != MetaType.VOID;
    }

}
