package org.swdc.clang.framework;

import org.swdc.clang.framework.def.*;
import org.swdc.libclang.core.CXType;
import org.swdc.libclang.core.LibClang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClangDeclaredContext implements ClangContext {

    private Map<String, NativeStructType> declaredStructs = new HashMap<>();

    private Map<String, NativeEnumType> declaredEnums = new HashMap<>();

    private Map<String, NativeFunction> declareFunctions = new HashMap<>();

    private Map<String, NativeConstant> constantMap = new HashMap<>();

    private Map<String, NativeClassType> declaredClasses = new HashMap<>();

    private Map<String, NativePointerType> declaredPointers = new HashMap<>();

    private Map<String, NativeArrayType> declaredArrays = new HashMap<>();

    public ClangDeclaredContext() {

    }

    @Override
    public void addDeclaredClass(NativeClassType classType) {
        declaredClasses.put(classType.getModifierName(), classType);
    }

    @Override
    public void addConstant(NativeConstant constant) {
        constantMap.put(constant.getName(), constant);
    }

    @Override
    public void removeConstant(NativeConstant constant) {
        constantMap.remove(constant.getName());
    }

    @Override
    public void addDeclaredStruct(NativeStructType struct) {
        declaredStructs.put(struct.getModifierName(), struct);
    }

    @Override
    public void removeDeclaredStruct(NativeStructType struct) {
        declaredStructs.remove(struct.getModifierName());
    }

    @Override
    public void addDeclaredEnum(NativeEnumType nativeEnumType) {
        declaredEnums.put(nativeEnumType.getModifierName(), nativeEnumType);
    }

    @Override
    public void addDeclaredPointer(NativePointerType pointer) {
        declaredPointers.put(pointer.getModifierName(), pointer);
    }

    @Override
    public void removeDeclaredPointer(NativePointerType pointer) {
        declaredPointers.remove(pointer.getModifierName());
    }

    @Override
    public void addDeclaredArray(NativeArrayType array) {
        declaredArrays.put(array.getModifierName(), array);
    }

    @Override
    public void removeDeclaredArray(NativeArrayType array) {
        declaredArrays.remove(array.getModifierName());
    }

    @Override
    public void removeDeclaredEnum(NativeEnumType nativeEnumType) {
        declaredEnums.remove(nativeEnumType.getModifierName());
    }

    @Override
    public void addDeclaredFunction(NativeFunction function) {
        declareFunctions.put(function.getModifierName(), function);
    }

    @Override
    public void removeDeclaredFunction(NativeFunction function) {
        declareFunctions.remove(function.getModifierName());
    }


    @Override
    public <T extends AbstractNativeType> T getType(CXType type) {

        String typeName = ClangUtils.getModifierName(type);
        return getType(
                typeName,
                LibClang.clang_isConstQualifiedType(type) == 1,
                LibClang.clang_isVolatileQualifiedType(type) == 1
        );

    }

    @Override
    public void addDeclaredType(AbstractNativeType type) {
        if (type instanceof NativeStructType) {
            addDeclaredStruct((NativeStructType) type);
        } else if (type instanceof NativeEnumType) {
            addDeclaredEnum((NativeEnumType) type);
        } else if (type instanceof NativeClassType) {
            addDeclaredClass((NativeClassType) type);
        } else if (type instanceof NativeFunction) {
            addDeclaredFunction((NativeFunction) type);
        } else if (type instanceof NativePointerType) {
            addDeclaredPointer((NativePointerType) type);
        } else if (type instanceof NativeArrayType) {
            addDeclaredArray((NativeArrayType) type);
        } else {
            throw new RuntimeException("Unknown type: " + type.getClass().getName());
        }
    }

    @Override
    public <T extends AbstractNativeType> T getType(String targetName, boolean isConst, boolean isVolatile) {

        String typeName = ClangUtils.getModifierName(targetName, isConst, isVolatile);

        if (declaredStructs.containsKey(typeName)) {
            return (T)declaredStructs.get(typeName);
        }
        if (declaredEnums.containsKey(typeName)) {
            return (T)declaredEnums.get(typeName);
        }
        if (declaredClasses.containsKey(typeName)) {
            return (T)declaredClasses.get(typeName);
        }
        if (declareFunctions.containsKey(typeName)) {
            return (T)declareFunctions.get(typeName);
        }
        if (declaredPointers.containsKey(typeName)) {
            return (T)declaredPointers.get(typeName);
        }

        return (T)BasicType.getByName(typeName);

    }

    @Override
    public List<NativeFunction> getDeclaredFunctions() {
        return new ArrayList<>(declareFunctions.values());
    }

    @Override
    public List<NativeClassType> getDeclaredClasses() {
        return new ArrayList<>(declaredClasses.values());
    }

    @Override
    public List<NativeStructType> getDeclaredStructs() {
        return new ArrayList<>(declaredStructs.values());
    }

    @Override
    public List<NativeEnumType> getDeclaredEnums() {
        return new ArrayList<>(declaredEnums.values());
    }

    @Override
    public List<NativeConstant> getDeclaredConstants() {
        return new ArrayList<>(constantMap.values());
    }


}
