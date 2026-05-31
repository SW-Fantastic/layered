package org.swdc.clang.framework.def;

import org.swdc.layered.def.NativeDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NativeEnumType extends AbstractNativeType {

    private Map<String, NativeEnumField> enumValues = new HashMap<>();

    public NativeEnumType(String name) {
        super(name, NativeDefinition.ENUM);
    }

    public void addEnumValue(String name, long value) {
        NativeEnumField enumField = new NativeEnumField();
        enumField.setName(name);
        enumField.setValue(value);
        enumValues.put(name, enumField);
    }

    public List<NativeEnumField> getEnumValues() {
        return enumValues.values().stream()
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public <T extends AbstractNativeType> T copy() {

        NativeEnumType target = new NativeEnumType(getName());
        target.setConstType(isConstType());
        target.setVolatileType(isVolatileType());

        for (NativeEnumField enumField : getEnumValues()) {
            target.addEnumValue(enumField.getName(), enumField.getValue());
        }

        return (T) target;

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

    @Override
    public String toString() {
        return "NativeEnumType{" +
                "name='" + getName() + '\'' +
                ", enumValues=" + enumValues.values().stream().map(Object::toString).reduce((sA,sB) -> sA + "," + sB).orElse("None") +
                '}';
    }
}
