package org.swdc.clang.framework.visitors;

import org.swdc.clang.framework.ClangContext;
import org.swdc.clang.framework.ClangDeclaredContext;
import org.swdc.clang.framework.ClangUtils;
import org.swdc.clang.framework.ParseExceptionElement;
import org.swdc.clang.framework.def.*;
import org.swdc.libclang.core.*;
import org.swdc.libclang.core.io.CXString;

import java.util.ArrayList;
import java.util.List;


public class ClangStructVisitor extends ClangDispatchVisitor {

    private NativeStructType structType;

    private List<ParseExceptionElement> exceptions = new ArrayList<>();

    public ClangStructVisitor(ClangContext context, NativeStructType struct) {
        super(context);
        this.structType = struct;
    }

    @Override
    public int call(CXCursor cxCursor, CXCursor parent, CXClientData cxClientData) {

        // 当前的Cursor的Token名称
        CXString name = LibClang.clang_getCursorDisplayName(cxCursor);
        String cursorName = ClangUtils.asString(name);
        ClangUtils.disposeStrings(name);

        if (cxCursor.kind() == LibClang.CXCursor_FieldDecl) {
            // 这是一个字段。
            CXType cxFieldType = LibClang.clang_getCursorType(cxCursor);
            AbstractNativeType fieldType = getByType(cxFieldType, cxClientData);
            if (fieldType == null) {
                return LibClang.CXChildVisit_Continue;
            }

            NativeField field = new NativeField();
            field.setName(cursorName);
            field.setType(fieldType);

            structType.addField(field);

            return LibClang.CXChildVisit_Continue;

        } else if (cxCursor.kind() == LibClang.CXCursor_CXXMethod) {

            // 隶属于Struct的方法，暂时不处理。
            return LibClang.CXChildVisit_Continue;

        } else {
            // 其他结构，例如嵌套在内部的子Struct类型之类的，代理给分发Visitor。
            return dispatchParse(cursorName, cxCursor, cxClientData);
        }

    }


    public List<ParseExceptionElement> getExceptions() {
        return exceptions;
    }
}
