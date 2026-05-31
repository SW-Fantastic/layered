package org.swdc.clang.framework;

import org.swdc.clang.framework.def.*;
import org.swdc.libclang.core.CXType;

import java.util.List;

public interface ClangContext {

    void addDeclaredClass(NativeClassType classType);

    void addConstant(NativeConstant constant);

    void removeConstant(NativeConstant constant);

    void addDeclaredStruct(NativeStructType struct);

    void removeDeclaredStruct(NativeStructType struct);

    void addDeclaredEnum(NativeEnumType nativeEnumType);

    void addDeclaredPointer(NativePointerType pointer);

    void removeDeclaredPointer(NativePointerType pointer);

    void addDeclaredArray(NativeArrayType array);

    void removeDeclaredArray(NativeArrayType array);

    void removeDeclaredEnum(NativeEnumType nativeEnumType);

    void addDeclaredFunction(NativeFunction function);

    void removeDeclaredFunction(NativeFunction function);

    <T extends AbstractNativeType> T getType(CXType type);

    void addDeclaredType(AbstractNativeType type);

    <T extends AbstractNativeType> T getType(String targetName, boolean isConst, boolean isVolatile);

    List<NativeFunction> getDeclaredFunctions();

    List<NativeClassType> getDeclaredClasses();

    List<NativeStructType> getDeclaredStructs();

    List<NativeEnumType> getDeclaredEnums();

    List<NativeConstant> getDeclaredConstants();
}
