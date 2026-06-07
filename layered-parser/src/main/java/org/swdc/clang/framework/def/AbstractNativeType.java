package org.swdc.clang.framework.def;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.swdc.clang.framework.ClangUtils;
import org.swdc.layered.def.NativeDefinition;

@JsonIdentityInfo(
        generator = ObjectIdGenerators.UUIDGenerator.class
)
public abstract class AbstractNativeType {

    /**
     * 类型名称
     */
    private String name;

    /**
     * 基础类型的元类型枚举值，例如：CLASS, STRUCT, ENUM, FUNCTION等。
     */
    private NativeDefinition type;

    /**
     * 是否为常量类型，例如：const int, const char*等。
     */
    private boolean constType;

    /**
     * 是否为volatile类型，例如：volatile int, volatile char*等。
     */
    private boolean volatileType;

    public AbstractNativeType(String name, NativeDefinition type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public NativeDefinition getType() {
        return type;
    }


    /**
     * 本类型的基础类型形式，例如，指针和数组对外表现为Long
     * @return 基础类型形式的字符串表示，例如：long, int等。
     */
    public NameCaster castAsBaseType() {
        return (name) -> {
            if (name == null || name.isEmpty()) {
                return "long";
            }
            return "long " + name;
        };
    }

    /**
     * 本类型的C/C++的类型形式。
     * 例如：指针应该为类型名称 + “*”，数组应该为类型名称 + “[]”等。
     * @return 本类型的C/C++形式
     */
    public NameCaster castFromBaseType() {
        return (name) -> {
            StringBuilder castBuilder = new StringBuilder();
            if (isConstType()) {
                castBuilder.append("const ");
            }
            if (isVolatileType()) {
                castBuilder.append("volatile ");
            }
            if (name == null || name.isEmpty()) {
                castBuilder.append(getName());
            } else {
                castBuilder.append(getName()).append(" ").append(name);
            }
            return castBuilder.toString();
        };
    }

    public boolean isConstType() {
        return constType;
    }

    public void setConstType(boolean constType) {
        this.constType = constType;
    }

    public boolean isVolatileType() {
        return volatileType;
    }

    public void setVolatileType(boolean volatileType) {
        this.volatileType = volatileType;
    }

    public String getModifierName() {
        return ClangUtils.getModifierName(getName(), isConstType(), isVolatileType());
    }

    public <T extends AbstractNativeType> T copy() {
        throw new UnsupportedOperationException();
    }

}
