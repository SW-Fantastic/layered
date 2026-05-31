package org.swdc.clang.framework.visitors;

import org.swdc.clang.framework.ClangContext;
import org.swdc.clang.framework.ClangDeclaredContext;
import org.swdc.clang.framework.ClangUtils;
import org.swdc.clang.framework.ParseExceptionElement;
import org.swdc.clang.framework.def.*;
import org.swdc.libclang.core.*;
import org.swdc.libclang.core.io.CXFile;
import org.swdc.libclang.core.io.CXSourceLocation;
import org.swdc.libclang.core.io.CXString;
import org.swdc.libclang.core.io.ClangIO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClangGlobalVisitor extends ClangDispatchVisitor {

    private File targetFile;

    private List<ParseExceptionElement> exceptionElements = new ArrayList<>();

    public ClangGlobalVisitor(ClangContext context, File targetFile) {
        super(context);
        this.targetFile = targetFile;
    }

    @Override
    public int call(CXCursor cxCursor, CXCursor parent, CXClientData cxClientData) {

        // 解析全局范围的函数和自定义类型。
        // Clang会把include的内容混入当前的文件，所以需要过滤掉不属于本header的组成部分。

        // 通过获取文件路径的方式可以判断目前解析的内容来自哪里。
        CXString name = LibClang.clang_getCursorDisplayName(cxCursor);
        String cursorName = ClangUtils.asString(name);
        ClangUtils.disposeStrings(name);

        if (cursorName == null) {
            System.out.println("Warning: cursor name is null");
            return LibClang.CXChildVisit_Continue;
        }

        CXFile file = new CXFile();
        int[] line = new int[1];
        int[] column = new int[1];
        int[] offset = new int[1];
        CXSourceLocation location = LibClang.clang_getCursorLocation(cxCursor);
        ClangIO.clang_getSpellingLocation(location, file, line, column, offset);
        if (file.isNull()) {
            // 无法获取到文件路径，直接忽略。
            return LibClang.CXChildVisit_Continue;
        } else {

            CXString currentPath = ClangIO.clang_File_tryGetRealPathName(file);
            if (currentPath == null || currentPath.isNull()) {
                return LibClang.CXChildVisit_Continue;
            }
            String filePath = ClangUtils.asString(currentPath);
            String absPath = targetFile.toPath().normalize().toAbsolutePath().toString();
            ClangUtils.disposeStrings(currentPath);
            if (!absPath.equals(filePath)) {
                // 不是指定的header，直接忽略。
                return LibClang.CXChildVisit_Continue;
            }

        }

        // C语言中，header通常包含类型定义（Struct，Union，Enum）和函数。
       if (cxCursor.kind() == LibClang.CXCursor_TypedefDecl) {

            // 单独处置Typedef，防止出现Typedef作为嵌套的匿名结构体或者类型的唯一名称的时候，
            // 解析类型导致名称完全丢失的问题。
            CXType definedType = LibClang.clang_getCursorType(cxCursor);
            CXType realType = ClangUtils.getRealType(definedType,false);
            CXCursor realTypeCursor = LibClang.clang_getTypeDeclaration(realType);
            if (LibClang.clang_Cursor_isAnonymous(realTypeCursor) == 1) {
                // 匿名的，只能解析到类型外部的Typedef，让它的名称作为类型名称。
                cursorName = ClangUtils.getTypeName(definedType, true);
                return dispatchParse(cursorName, realTypeCursor, cxClientData);
            }
            // 具名的结构最终会被解释为基本类型，这里跳过即可。
            return LibClang.CXChildVisit_Continue;
       } else if (cxCursor.kind() == LibClang.CXCursor_FunctionDecl) {
           // 处理全局函数
            return doParseFunction(cursorName, cxCursor,cxClientData);
       } else {
           // 处理其他结构
            return dispatchParse(cursorName, cxCursor, cxClientData);
       }

    }

    private int doParseFunction(String functionDeclare, CXCursor cxCursor, CXClientData cxClientData) {

        String functionName = functionDeclare.substring(0, functionDeclare.indexOf('('));
        NativeFunction function = new NativeFunction(functionName);

        ClangFuncParamVisitor funcVisitor = new ClangFuncParamVisitor(getContext());
        LibClang.clang_visitChildren(cxCursor, funcVisitor, cxClientData);
        funcVisitor.close();

        CXType functionType = LibClang.clang_getCursorType(cxCursor);
        CXType cxReturnType = LibClang.clang_getResultType(functionType);

        List<NativeFunctionParam> params = funcVisitor.getParams();
        if (params == null) {
            params = new ArrayList<>();
        }
        int paramsCount = LibClang.clang_getNumArgTypes(functionType);
        if (paramsCount != params.size()) {
            // 尽可能获取完整的参数名称，实在没有就直接使用index。
            for (int index = 0; index < paramsCount; ++index) {

                CXType paramType = LibClang.clang_getArgType(functionType, index);
                AbstractNativeType nativeParamType = getByType(paramType, cxClientData);
                if (nativeParamType == null) {
                    exceptionElements.add(ClangUtils.parseException("Cannot parse parameter type for " + functionName, cxCursor));
                    return LibClang.CXChildVisit_Continue;
                }

                NativeFunctionParam param = new NativeFunctionParam();
                param.setIndex(index);
                param.setType(nativeParamType);
                param.setName("arg" + index);
                params.add(param);

            }
        }
        function.addFunctionParameterTypes(params);

        AbstractNativeType returnType = getByType(cxReturnType, cxClientData);
        if (returnType == null) {
            returnType = dispatchParse(cxReturnType,cxClientData);
            if (returnType == null) {
                exceptionElements.add(ClangUtils.parseException("Cannot parse return type for " + functionName, cxCursor));
            }
            return LibClang.CXChildVisit_Continue;
        }

        TypeParameterized returnParam = new TypeParameterized();
        returnParam.setName("result");
        returnParam.setType(returnType);
        function.setReturnType(returnParam);

        System.out.println(function);
        getContext().addDeclaredFunction(function);
        return LibClang.CXChildVisit_Continue;
    }

    public List<ParseExceptionElement> getExceptionElements() {
        return exceptionElements;
    }
}
