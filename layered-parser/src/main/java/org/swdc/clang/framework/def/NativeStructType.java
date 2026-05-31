package org.swdc.clang.framework.def;

import org.swdc.layered.def.NativeDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Struct 类型定义
 * 这里只记录Struct本身相关的内容，即它的字段和函数。
 * Struct的嵌套关系本身通过Struct的name表达，它是一个全限定名，因此可以很好的体现
 * 一个Struct的逻辑上的作用域，包括Struct内的其他结构都可以这样表达，所以，这里不记录嵌套的关系。
 */
public class NativeStructType extends AbstractNativeType {

    private Map<String, NativeField> fields = new HashMap<>();

    private Map<String, NativeFunction> functions = new HashMap<>();

    public NativeStructType(String name) {
        super(name, NativeDefinition.STRUCT);
    }

    public void addField(NativeField field) {
        fields.put(field.getName(), field);
    }

    public void addFunction(NativeFunction function) {
        functions.put(function.getName(), function);
    }

    public List<NativeFunction> getFunctions() {
        return functions.values().stream()
                .collect(Collectors.toUnmodifiableList());
    }

    public List<NativeField> getFields() {
        return fields.values().stream()
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public <T extends AbstractNativeType> T copy() {

        NativeStructType struct = new NativeStructType(getName());
        struct.setConstType(isConstType());
        struct.setVolatileType(isVolatileType());

        for (Map.Entry<String, NativeField> entry : fields.entrySet()) {

            NativeField field = entry.getValue();
            NativeField fieldCopy = new NativeField();

            fieldCopy.setName(field.getName());
            fieldCopy.setType(field.getType().copy());
            struct.addField(fieldCopy);
        }

        for (Map.Entry<String, NativeFunction> entry : functions.entrySet()) {

            NativeFunction function = entry.getValue();
            NativeFunction functionCopy = new NativeFunction(function.getName());

            for (NativeFunctionParam param : function.getParameters()) {

                NativeFunctionParam paramCopy = new NativeFunctionParam();
                paramCopy.setName(param.getName());
                paramCopy.setType(param.getType());
                paramCopy.setIndex(param.getIndex());
                functionCopy.addFunctionParameterType(paramCopy);

            }

            functionCopy.setReturnType(function.getReturnType());
            struct.addFunction(functionCopy);
        }

        return (T) struct;
    }

    @Override
    public String toString() {
        return "NativeStructType{" +
                "name='" + getName() + '\'' +
                ", fields=" + fields.values().stream().map(Object::toString)
                    .reduce( (f, f2) -> f + "," + f2)
                    .orElse("None") +
                ", functions=" + functions.values().stream().map(Object::toString)
                        .reduce( (f, f2) -> f + "," + f2)
                        .orElse("None") +
                "}";
    }
}
