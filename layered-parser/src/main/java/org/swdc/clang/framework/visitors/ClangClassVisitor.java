package org.swdc.clang.framework.visitors;

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

    private CXCursor scopeCursor;

    public ClangClassVisitor(ClangDeclaredContext context, CXCursor classCursor, String className) {

        super(context);


    }

    private void doParseClassMeta(CXClientData cxClientData) {

    }

    @Override
    public int call(CXCursor cursor, CXCursor parent, CXClientData client_data) {

        return LibClang.CXChildVisit_Continue;

    }

}
