package org.swdc.clang.framework.visitors;

import org.swdc.clang.framework.ClangContext;
import org.swdc.clang.framework.ClangDeclaredContext;
import org.swdc.clang.framework.ClangUtils;
import org.swdc.libclang.core.*;
import org.swdc.libclang.core.io.CXString;

public class ClangNamespaceVisitor extends ClangDispatchVisitor {

    public ClangNamespaceVisitor(ClangContext context) {
        super(context);
    }

    @Override
    public int call(CXCursor cxCursor, CXCursor parent, CXClientData client_data) {

        CXString name = LibClang.clang_getCursorDisplayName(cxCursor);
        String cursorName = ClangUtils.asString(name);
        ClangUtils.disposeStrings(name);
        return dispatchParse(cursorName,cxCursor,client_data);

    }

}
