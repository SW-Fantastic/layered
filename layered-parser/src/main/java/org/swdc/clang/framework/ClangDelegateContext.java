package org.swdc.clang.framework;

import org.swdc.clang.framework.def.*;
import org.swdc.libclang.core.CXType;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClangDelegateContext implements ClangContext {

    private Map<File, ClangContext> contexts = new HashMap<>();

    private File sourceFile;

    public void withSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    private ClangContext getWritableContext() {
        if (sourceFile == null) {
            throw new IllegalStateException("Source file has not been set");
        }
        ClangContext context = contexts.get(sourceFile);
        if (context == null) {
            context = new ClangDeclaredContext();
            contexts.put(sourceFile, context);
        }
        return context;
    }

    @Override
    public void addDeclaredClass(NativeClassType classType) {

        ClangContext context = getWritableContext();
        context.addDeclaredClass(classType);

    }

    @Override
    public void addConstant(NativeConstant constant) {
        ClangContext context = getWritableContext();
        context.addConstant(constant);
    }

    @Override
    public void removeConstant(NativeConstant constant) {
        ClangContext context = getWritableContext();
        context.removeConstant(constant);
    }

    @Override
    public void addDeclaredStruct(NativeStructType struct) {
        ClangContext context = getWritableContext();
        context.addDeclaredStruct(struct);
    }

    @Override
    public void removeDeclaredStruct(NativeStructType struct) {
        ClangContext context = getWritableContext();
        context.removeDeclaredStruct(struct);
    }

    @Override
    public void addDeclaredEnum(NativeEnumType nativeEnumType) {
        ClangContext context = getWritableContext();
        context.addDeclaredEnum(nativeEnumType);
    }

    @Override
    public void addDeclaredPointer(NativePointerType pointer) {
        ClangContext context = getWritableContext();
        context.addDeclaredPointer(pointer);
    }

    @Override
    public void removeDeclaredPointer(NativePointerType pointer) {
        ClangContext context = getWritableContext();
        context.removeDeclaredPointer(pointer);
    }

    @Override
    public void addDeclaredArray(NativeArrayType array) {
        ClangContext context = getWritableContext();
        context.addDeclaredArray(array);
    }

    @Override
    public void removeDeclaredArray(NativeArrayType array) {
        ClangContext context = getWritableContext();
        context.removeDeclaredArray(array);
    }

    @Override
    public void removeDeclaredEnum(NativeEnumType nativeEnumType) {
        ClangContext context = getWritableContext();
        context.removeDeclaredEnum(nativeEnumType);
    }

    @Override
    public void addDeclaredFunction(NativeFunction function) {
        ClangContext context = getWritableContext();
        context.addDeclaredFunction(function);
    }

    @Override
    public void removeDeclaredFunction(NativeFunction function) {
        ClangContext context = getWritableContext();
        context.removeDeclaredFunction(function);
    }

    @Override
    public <T extends AbstractNativeType> T getType(CXType type) {
        for (ClangContext context : contexts.values()) {
            Object object = context.getType(type);
            if (object != null) {
                return (T)object;
            }
        }
        return null;
    }

    @Override
    public void addDeclaredType(AbstractNativeType type) {
        ClangContext context = getWritableContext();
        context.addDeclaredType(type);
    }

    @Override
    public <T extends AbstractNativeType> T getType(String targetName, boolean isConst, boolean isVolatile) {
        for (ClangContext context : contexts.values()) {
            Object object = context.getType(targetName, isConst, isVolatile);
            if (object != null) {
                return (T)object;
            }
        }
        return null;
    }

    @Override
    public List<NativeFunction> getDeclaredFunctions() {
        return contexts.values().stream()
                .flatMap(s -> s.getDeclaredFunctions().stream())
                .collect(Collectors.toList());

    }

    @Override
    public List<NativeClassType> getDeclaredClasses() {
        return contexts.values().stream()
                .flatMap(s -> s.getDeclaredClasses().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<NativeStructType> getDeclaredStructs() {
        return contexts.values().stream()
                .flatMap(s -> s.getDeclaredStructs().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<NativeEnumType> getDeclaredEnums() {
        return contexts.values().stream()
                .flatMap(s -> s.getDeclaredEnums().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<NativeConstant> getDeclaredConstants() {
        return contexts.values().stream()
                .flatMap(s -> s.getDeclaredConstants().stream())
                .collect(Collectors.toList());
    }

    public ClangContext getContext(File file) {
        if (contexts.containsKey(file)) {
            return contexts.get(file);
        }
        return null;
    }


}
