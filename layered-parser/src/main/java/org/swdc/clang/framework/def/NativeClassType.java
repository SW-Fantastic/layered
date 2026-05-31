package org.swdc.clang.framework.def;

import org.swdc.layered.def.NativeDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NativeClassType  extends AbstractNativeType {

    private Map<String, NativeFunction> publicMethods = new HashMap<>();

    private Map<String, NativeFunction> protectedMethods = new HashMap<>();

    private Map<String, NativeField> fields = new HashMap<>();

    private List<NativeClassType> superClasses = new ArrayList<>();

    private List<AbstractNativeType> templateArguments = new ArrayList<>();

    public NativeClassType(String name) {
        super(name, NativeDefinition.CLASS);
    }

    public void addSuperClass(NativeClassType superClass) {
        superClasses.add(superClass);
    }

    public void addPublicMethod(NativeFunction method) {
        publicMethods.put(method.getName(), method);
    }

    public void addProtectedMethod(NativeFunction method) {
        protectedMethods.put(method.getName(), method);
    }

    public void addTemplateArgument(AbstractNativeType templateArg) {
        templateArguments.add(templateArg);
    }

    public void addField(NativeField field) {
        fields.put(field.getName(), field);
    }


    @Override
    public String toString() {
        return "NativeClassType{" +
                "name='" + getName() + '\'' +
                ", publicMethods=" + publicMethods.keySet() +
                ", protectedMethods=" + protectedMethods.keySet() +
                ", fields=" + fields.keySet() +
                '}';
    }
}
