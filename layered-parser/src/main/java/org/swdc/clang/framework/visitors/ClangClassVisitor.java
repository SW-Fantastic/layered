package org.swdc.clang.framework.visitors;

import org.swdc.clang.framework.ClangContext;
import org.swdc.clang.framework.ClangDeclaredContext;
import org.swdc.clang.framework.ClangUtils;
import org.swdc.clang.framework.def.*;
import org.swdc.libclang.core.CXClientData;
import org.swdc.libclang.core.CXCursor;
import org.swdc.libclang.core.CXType;
import org.swdc.libclang.core.LibClang;
import org.swdc.libclang.core.io.CXString;

import java.util.List;

public class ClangClassVisitor extends ClangDispatchVisitor {

    private NativeClassType classType;

    private String className;

    public ClangClassVisitor(ClangContext context, CXCursor classCursor, String className) {
        super(context);
        this.className = className;
    }

    private void doParseClassMeta(CXClientData cxClientData) {

    }

    @Override
    public int call(CXCursor cursor, CXCursor parent, CXClientData client_data) {

        CXString cursorKind = LibClang.clang_getCursorKindSpelling(cursor.kind());
        CXString spell = LibClang.clang_getCursorSpelling(cursor);
        System.out.println("Cursor name: " + ClangUtils.asString(spell) + " kind is " + ClangUtils.asString(cursorKind));
        ClangUtils.disposeStrings(cursorKind,spell);

        return LibClang.CXChildVisit_Continue;
    }

}
