package org.swdc.clang.framework.def;

import org.swdc.layered.def.NativeDefinition;

public class NativePointerType extends AbstractNativeType {

    /**
     * 指针的目标类型。例如：int* 的 pointeeType 就是 BasicType.INT。
     */
    private AbstractNativeType pointeeType;

    /**
     * 指针的级别，例如：int* 就是1级， int** 是2级。
     */
    private int pointerLevel;

    public NativePointerType(AbstractNativeType target, int pointerLevel) {
        super("_Ptr" + pointerLevel + "_" + target.getModifierName(), NativeDefinition.POINTER);
        pointeeType = target;
        this.pointerLevel = pointerLevel;
        if (target instanceof NativePointerType) {
            throw new IllegalArgumentException("please use pointer level.");
        }
    }

    public int getPointerLevel() {
        return pointerLevel;
    }

    public AbstractNativeType getPointeeType() {
        return pointeeType;
    }

    public void setPointeeType(AbstractNativeType pointeeType) {
        this.pointeeType = pointeeType;
    }

    @Override
    public <T extends AbstractNativeType> T copy() {

        NativePointerType pointerType = new NativePointerType(pointeeType, pointerLevel);
        pointerType.setConstType(isConstType());
        pointerType.setVolatileType(isVolatileType());
        return (T) pointerType;

    }

    @Override
    public NameCaster castAsBaseType() {
        return (name) -> {
            if (name == null || name.isEmpty()) {
                return "intptr_t";
            }
            return "intptr_t " + name;
        };
    }

    @Override
    public NameCaster castFromBaseType() {
        return (name) -> {

            boolean isFunctionPointer = pointeeType instanceof NativeFunction;

            String targetName = pointeeType.castFromBaseType().as("");
            if (isFunctionPointer) {
                targetName = pointeeType.castFromBaseType().as(name == null || name.isEmpty() ? "" : name);
            }
            StringBuilder castBuilder = new StringBuilder();
            if (pointeeType.isConstType()) {
                castBuilder.append("const ");
                targetName = targetName.replace("const ", "");
            }

            if (pointeeType.isVolatileType()) {
                castBuilder.append("volatile ");
                targetName = targetName.replace("volatile ", "");
            }

            castBuilder.append(targetName.trim());
            if (!isFunctionPointer) {
                castBuilder.append("*".repeat(pointerLevel));
            }
            if (isConstType()) {
                castBuilder.append("const ");
            }
            if (isVolatileType()) {
                castBuilder.append("volatile ");
            }
            if (name != null && !name.isEmpty() && !isFunctionPointer) {
                castBuilder.append(name);
            }
            return castBuilder.toString();
        };
    }
}
