package org.swdc.clang.framework;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.CharPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.swdc.clang.framework.visitors.ClangGlobalVisitor;
import org.swdc.libclang.core.*;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CLangParser {

    private List<File> includeDirs;

    private List<String> args;

    private ClangDelegateContext context;

    private List<File> headers = new ArrayList<>();

    public CLangParser(List<String> args, List<File> includeDirs) {

        this.args = (args == null) ? Collections.emptyList() : args;
        this.includeDirs = (includeDirs == null) ? Collections.emptyList() : includeDirs;
        this.context = new ClangDelegateContext();

    }

    public CLangParser addHeader(File headerFile) {
        headers.add(headerFile);
        return this;
    }

    public CLangParser addHeaders(File ...headerFiles) {
        for (File headerFile : headerFiles) {
            addHeader(headerFile);
        }
        return this;
    }

    public void parse() {

        CXIndex index = LibClang.clang_createIndex(0, 1);
        String[] args = new String[includeDirs.size() + this.args.size()];
        for (int i = 0; i < this.args.size(); ++i) {
            args[i] = this.args.get(i);
        }
        for (int i = 0; i < this.includeDirs.size(); ++i) {
            args[this.args.size() + i] = "-I" + includeDirs.get(i).getAbsolutePath();
        }
        PointerPointer pArgs = asPointerArray(args);

        for (int i = 0; i < this.headers.size(); ++i) {
            File parseHeaderFile = headers.get(i);
            BytePointer pTargetHeader = asPointer(parseHeaderFile.getAbsolutePath());

            CXTranslationUnitImpl unit = LibClang.clang_parseTranslationUnit(
                    index,
                    pTargetHeader,
                    pArgs,
                    args.length,
                    null,
                    0,
                    LibClang.CXTranslationUnit_None
            );

            context.withSourceFile(parseHeaderFile);
            ClangGlobalVisitor visitor = new ClangGlobalVisitor(context, parseHeaderFile);
            CXCursor cxCursor = LibClang.clang_getTranslationUnitCursor(unit);
            LibClang.clang_visitChildren(cxCursor, visitor, null);
            visitor.close();
            cxCursor.close();

            LibClang.clang_disposeTranslationUnit(unit);
            Pointer.free(pTargetHeader);
        }

        for (int i = 0; i < args.length; i++) {
            Pointer.free(pArgs.get(i));
        }
        Pointer.free(pArgs);
        LibClang.clang_disposeIndex(index);
    }

    private BytePointer asPointer(String str) {
        BytePointer pointer = new BytePointer(Pointer.malloc(str.length() * Pointer.sizeof(CharPointer.class)));
        Pointer.memset(pointer, 0, str.length());
        pointer.putString(str);
        return pointer;
    }

    private PointerPointer asPointerArray(String[] str) {

        long size = (long) str.length * Pointer.sizeof(Pointer.class);
        PointerPointer pp = new PointerPointer(Pointer.malloc(size));
        Pointer.memset(pp, 0, size);
        for (int i = 0; i < str.length; ++i) {
            pp.put(i, asPointer(str[i]));
        }
        return pp;

    }

    public ClangContext getContext() {
        return context;
    }

    public ClangContext getContext(File sourceFile) {
        return context.getContext(sourceFile);
    }

    public List<File> getHeaders() {
        return headers;
    }
}
