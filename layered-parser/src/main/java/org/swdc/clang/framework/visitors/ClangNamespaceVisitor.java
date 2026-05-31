package org.swdc.clang.framework.visitors;

import org.swdc.clang.framework.ClangDeclaredContext;
import org.swdc.clang.framework.ClangUtils;
import org.swdc.libclang.core.*;
import org.swdc.libclang.core.io.CXString;

public class ClangNamespaceVisitor extends ClangDispatchVisitor {

    public ClangNamespaceVisitor(ClangDeclaredContext context) {
        super(context);
    }

    @Override
    public int call(CXCursor cxCursor, CXCursor parent, CXClientData client_data) {

        CXString name = LibClang.clang_getCursorDisplayName(cxCursor);
        String cursorName = ClangUtils.asString(name);
        ClangUtils.disposeStrings(name);

        if (cxCursor.kind() == LibClang.CXCursor_TypedefDecl) {

            // 单独处置Typedef，防止出现Typedef作为嵌套的匿名结构体或者类型的唯一名称的时候，
            // 解析类型导致名称完全丢失的问题。
            CXType definedType = LibClang.clang_getCursorType(cxCursor);
            CXType realType = ClangUtils.getRealType(definedType,false);
            CXCursor realTypeCursor = LibClang.clang_getTypeDeclaration(realType);
            if (LibClang.clang_Cursor_isAnonymous(realTypeCursor) == 1) {
                // 匿名的，只能解析到类型外部的Typedef，让它的名称作为类型名称。
                cursorName = ClangUtils.getTypeName(definedType, true);
                return dispatchParse(cursorName, realTypeCursor, client_data);
            }
            // 具名的结构最终会被解释为基本类型，这里跳过即可。
            return LibClang.CXChildVisit_Continue;
        } else {
            return dispatchParse(cursorName, cxCursor, client_data);
        }

    }

}
