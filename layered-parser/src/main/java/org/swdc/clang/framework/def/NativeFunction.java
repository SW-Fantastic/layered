package org.swdc.clang.framework.def;

import org.swdc.clang.framework.ClangUtils;
import org.swdc.layered.def.NativeDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class NativeFunction extends AbstractNativeType {


    /**
     * 参数列表。
     */
    private List<NativeFunctionParam> parameters = new ArrayList<>();

    /**
     * 返回类型的描述信息
     */
    private TypeParameterized returnType;

    /**
     * 是否为实例方法。
     */
    private boolean instanceMethod = false;

    /**
     * 是否为构造函数。
     */
    private boolean constructor = false;

    /**
     * 是否为析构函数。
     */
    private boolean destructor = false;

    private boolean callback = false;

    public void setCallback(boolean callback) {
        this.callback = callback;
    }

    public boolean isCallback() {
        return callback;
    }

    public NativeFunction(String name) {
        super(name, NativeDefinition.FUNCTION);
    }


    public void setReturnType(TypeParameterized returnType) {
        this.returnType = returnType;
    }

    public TypeParameterized getReturnType() {
        return returnType;
    }

    public void setInstanceMethod(boolean instanceMethod) {
        this.instanceMethod = instanceMethod;
    }

    public boolean isInstanceMethod() {
        return instanceMethod;
    }

    public void addFunctionParameterType(NativeFunctionParam param) {
        parameters.add(param);
    }

    public void addFunctionParameterTypes(List<NativeFunctionParam> params) {
        parameters.addAll(params);
    }

    public List<NativeFunctionParam> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public void setConstructor(boolean constructor) {
        this.constructor = constructor;
    }

    public boolean isConstructor() {
        return constructor;
    }

    public void setDestructor(boolean destructor) {
        this.destructor = destructor;
    }

    public boolean isDestructor() {
        return destructor;
    }

    @Override
    public String getName() {
        return "(" + ClangUtils.generateMangled(Collections.singletonList(returnType),false) + ")" +
                super.getName() + "@" + ClangUtils.generateMangled(parameters,false);
    }

    public String getRawName() {
        return super.getName();
    }

    @Override
    public NameCaster castAsBaseType() {
        return (name) -> {
            if (name == null || name.isEmpty()) {
                return "long";
            }
            return "long " + name;
        };
    }

    public NameCaster castFromBaseType() {

        return (name) -> {

            List<NativeFunctionParam> params = parameters.stream()
                    .sorted(Comparator.comparingInt(NativeFunctionParam::getIndex))
                    .collect(Collectors.toList());

            StringBuilder paramStr = new StringBuilder("(");
            for (int index = 0; index < params.size(); index++) {
                NativeFunctionParam param = params.get(index);
                paramStr.append(param.getType().castFromBaseType().as(""));
                if (index != params.size() - 1) {
                    paramStr.append(",");
                }
            }

            paramStr.append(")");
            String ret = returnType.getType().castFromBaseType().as("");
            if (name == null || name.isEmpty()) {
                return ret + "(*)" + paramStr;
            }
            return ret + "(*" + name + ")" + paramStr;
        };
    }

    @Override
    public String toString() {
        return "NativeFunction{" +
                "name='" + getName() + '\'' +
                ", parameters=" + parameters.stream()
                    .map(p -> p.getType().getName())
                    .reduce((tA,tB) -> tA + "," + tB)
                    .orElse("None") +
                '}';
    }

}
