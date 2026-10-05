package org.swdc.clang.libtooling.context;

/**
 * 基本类型
 */
public class BuiltInMetaType extends AbstractMetaType {

    public BuiltInMetaType(MetaType metaType) {
        if (metaType == null) {
            throw new IllegalArgumentException("metaType cannot be null");
        } else if (metaType == MetaType.INTPTR_T) {
            throw new IllegalArgumentException("BuiltInMetaType cannot be INTPTR_T");
        }
        this.setRawType(metaType);
    }

    public boolean isUnsigned() {
        return (this.modifier & Modifiers.IsUnsigned) != 0;
    }

    public void setUnsigned(boolean isUnsigned) {
        this.modifier = this.modifier & ~Modifiers.UnsignedMask;
        if (isUnsigned) {
            this.modifier = this.modifier | Modifiers.IsUnsigned;
        }
    }

    @Override
    public String cppTypeFullName() {
        if (getName() == null || getName().isBlank()) {
            throw new IllegalArgumentException("BuiltInMetaType cannot be null or blank");
        }
        String cppModifier = MetaTypeUtils.cppTypeModifier(this);
        if (isUnsigned()) {
            cppModifier += " unsigned ";
        }
        return cppModifier + getRawType().getTypeName();
    }

    @Override
    public String getMangledName() {
        if (getName() == null || getName().isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        String cvModifier = "";
        if (isConst()) {
            cvModifier += "C";
        }
        if (isVolatile()) {
            cvModifier += "V";
        }
        String result = (isUnsigned() ? "U" : "") + getRawType().getTypeName();
        if (cvModifier.isBlank()) {
            return result;
        }
        return "[" + cvModifier + "]" + result;
    }

    @Override
    public String castFromRawType(String varName, String paramName) {

        if (varName == null || varName.isBlank() || paramName == null || paramName.isBlank()) {
            throw new  IllegalArgumentException("varName or paramName cannot be null or blank");
        }

        String targetType =  cppTypeFullName();
        return targetType + " " + varName + " = static_cast<" + targetType + ">(" + paramName + ");";

    }

    @Override
    public String castToRawType(String varName, String paramName) {

        if (varName == null || varName.isBlank() || paramName == null || paramName.isBlank()) {
            throw new  IllegalArgumentException("varName or paramName cannot be null or blank");
        }

        MetaType rawType = getRawType();
        return rawType.getTypeName() + " " + varName + " = static_cast<" + rawType.getTypeName() + ">(" + paramName + ");";

    }

}
