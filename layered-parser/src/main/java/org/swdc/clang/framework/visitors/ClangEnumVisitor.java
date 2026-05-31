package org.swdc.clang.framework.visitors;


import org.swdc.clang.framework.ClangUtils;
import org.swdc.clang.framework.def.NativeEnumType;
import org.swdc.libclang.core.CXClientData;
import org.swdc.libclang.core.CXCursor;
import org.swdc.libclang.core.CXCursorVisitor;
import org.swdc.libclang.core.LibClang;
import org.swdc.libclang.core.io.CXString;

/**
 * 专用于Enum的Visitor，它的主要作用是收集Enum的字段。
 */
public class ClangEnumVisitor extends CXCursorVisitor {

    private NativeEnumType enumType;


    public ClangEnumVisitor(NativeEnumType enumType) {
        this.enumType = enumType;
    }

    @Override
    public int call(CXCursor cursor, CXCursor parent, CXClientData client_data) {

        CXString name = LibClang.clang_getCursorDisplayName(cursor);
        String cursorName = ClangUtils.asString(name);
        if (cursor.kind() == LibClang.CXCursor_EnumConstantDecl) {
            Long value = LibClang.clang_getEnumConstantDeclValue(cursor);
            enumType.addEnumValue(cursorName, value);
        }
        ClangUtils.disposeStrings(name);
        return LibClang.CXChildVisit_Continue;
    }

}
