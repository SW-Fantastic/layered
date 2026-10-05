package org.swdc.clang.libtooling.context;

public abstract class AbstractMetaType {

    /**
     * 本类型在Wrapper里面使用的原始类型。
     */
    private MetaType rawType;

    /**
     * 本类型名称，
     * 并非每一种AbstractMetaType都有名称，例如指针类型就没有。
     * 确定性的获取函数的C++类型全名，需要使用cppTypeFullName()方法。
     */
    private String name;

    /**
     * 本类型的namespace/scope
     */
    private String namespace;

    /**
     * 本类型的各类描述。
     */
    protected int modifier = 0x00;

    public String getNamespace() {
        return namespace == null ? "" : namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MetaType getRawType() {
        return rawType;
    }

    protected void setRawType(MetaType rawType) {
        this.rawType = rawType;
    }

    public void setPublic(boolean isPublic) {
        // Modifiers.AccessorMask 变成 1110 0011，与之相与可以把中间三位清零
        this.modifier = this.modifier & ~Modifiers.AccessorMask;
        if (isPublic) {
            this.modifier = this.modifier | Modifiers.IsPublic;
        }
    }

    public void setPrivate(boolean isPrivate) {
        // Modifiers.AccessorMask 变成 1110 0011，与之相与可以把中间三位清零
        this.modifier = this.modifier & ~Modifiers.AccessorMask;
        if (isPrivate) {
            this.modifier = this.modifier | Modifiers.IsPrivate;
        }
    }

    public void setProtected(boolean isProtected) {
        // Modifiers.AccessorMask 变成 1110 0011，与之相与可以把中间三位清零
        this.modifier = this.modifier & ~Modifiers.AccessorMask;
        if (isProtected) {
            this.modifier = this.modifier | Modifiers.IsProtected;
        }
    }

    public void setConst(boolean isConst) {
        this.modifier = this.modifier & ~Modifiers.IsConst;
        if (isConst) {
            this.modifier = this.modifier | Modifiers.IsConst;
        }
    }

    public void setVolatile(boolean isVolatile) {
        this.modifier = this.modifier & ~Modifiers.IsVolatile;
        if (isVolatile) {
            this.modifier = this.modifier | Modifiers.IsVolatile;
        }
    }

    public boolean isPublic() {
        return (this.modifier & Modifiers.IsPublic) != 0;
    }

    public boolean isPrivate() {
        return (this.modifier & Modifiers.IsPrivate) != 0;
    }

    public boolean isProtected() {
        return (this.modifier & Modifiers.IsProtected) != 0;
    }

    public boolean isConst() {
        return (this.modifier & Modifiers.IsConst) != 0;
    }

    public boolean isVolatile() {
        return (this.modifier & Modifiers.IsVolatile) != 0;
    }

    /**
     * 将另一个元类型合并到当前实例。
     * 当前实现仅校验两个元类型的 mangled name 是否相同，如果相同则允许合并，
     * 若不同则抛出 IllegalStateException。
     *
     * 子类型需要自行重写本方法实现必要的合并逻辑。
     *
     * @param other 待合并的元类型
     * @throws IllegalStateException 当两个元类型的 mangled name 不同时
     */
    public void merge(AbstractMetaType other) {
        if (!other.getMangledName().equals(this.getMangledName())) {
            throw new  IllegalStateException("Cannot merge different meta types");
        }
    }

    /**
     * 本方法实现AbstractMetaType的唯一符号，对于函数来说，
     * 本方法是特殊的，它返回的包含函数返回值，参数表的特殊唯一Id。
     * @return MangledName
     */
    public abstract String getMangledName();

    /**
     * 本方法实现从基本类型转为C++类型的C++语句生成
     * @param varName 转换后使用的变量名
     * @param paramName 被转换的变量名
     * @return 类型转换语句
     */
    public abstract String castFromRawType(String varName,String paramName);

    /**
     * 本方法实现从C++类型转为基本类型的C++语句生成
     * @param varName 转换后使用的变量名
     * @param paramName 被转换的变量名
     * @return 类型转换语句
     */
    public abstract String castToRawType(String varName, String paramName);


    /**
     * 本方法以“借用”的方式实现从C++类型转为基本类型的C++语句生成，
     * 借用转换不会复制或转移原始数据的所属权，仅生成可供临时使用的转换语句。
     * 默认行为与 castToRawType 一致，由子类按需重写以提供引用借用语义。
     * @param varName 转换后使用的变量名
     * @param paramName 被转换的变量名
     * @return 类型转换语句
     */
    public String castToRawTypeBorrowed(String varName, String paramName) {
        return castToRawType(varName, paramName);
    }

    /**
     * 本方法确定性的返回包含修饰符的C++类型的完整名称，例如：const std::string
     * @return C++类型完整名称
     */
    public String cppTypeFullName() {
        if (getName() == null || getName().isBlank()) {
            throw new IllegalArgumentException("MetaType name cannot be null or blank");
        }
        String namespace = getNamespace();
        String typeModifier = MetaTypeUtils.cppTypeModifier(this);
        if (typeModifier.isBlank()) {
            if (namespace.isBlank()) {
                return getName();
            }
            return namespace + "::" + getName();
        } else if (namespace.isBlank()) {
            return typeModifier + getName();
        } else {
            return typeModifier + namespace + "::" + getName();
        }
    }

    public String cppTypeFullName(String varName) {
        return cppTypeFullName() + " " + varName;
    }


}
