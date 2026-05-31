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

import java.util.ArrayList;
import java.util.List;

/**
 * 这个Visitor专用于处理函数的参数。
 * 部分函数的参数可以包含其他的函数指针，所以有必要进行一个递归的解析处理。
 */
public class ClangFuncParamVisitor extends ClangDispatchVisitor {

    private int currentIndex;

    private List<NativeFunctionParam> params = new ArrayList<>();

    public ClangFuncParamVisitor(ClangContext context) {

        super(context);

    }

    @Override
    public int call(CXCursor cursor, CXCursor parent, CXClientData client_data) {

        if (cursor.kind() == LibClang.CXCursor_ParmDecl) {

            CXType paramType = LibClang.clang_getCursorType(cursor);
            // 只有参数名称，有可能会出现基础类型的指针被遗漏的问题。
            AbstractNativeType type = getByType(paramType,client_data);

            if (type == null) {
                params = null;
                return LibClang.CXChildVisit_Break;
            }

            CXString name = LibClang.clang_getCursorSpelling(cursor);
            String paramName = ClangUtils.asString(name);
            if (paramName.isBlank()) {
                paramName = "arg" + currentIndex;
            }

            NativeFunctionParam param = new NativeFunctionParam();
            param.setType(type);
            param.setName(paramName);
            param.setIndex(currentIndex);
            params.add(param);

            ClangUtils.disposeStrings(name);

            ++currentIndex;
        }

        return LibClang.CXChildVisit_Continue;
    }


    public List<NativeFunctionParam> getParams() {
        return params;
    }
}
