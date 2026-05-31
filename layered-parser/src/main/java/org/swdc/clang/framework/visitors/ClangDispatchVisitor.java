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
import java.util.Stack;

/**
 * 这是一个特殊的Visitor，它非常基础，主要的目标是为作用域选择一个合适的Visitor，
 * 解析完毕后，将解析的结果装入上下文中，以便后续的访问者可以基于上下文中的信息。
 *
 * 本Visitor不处理字段，函数以及常量，这是因为它们与其他结构强相关，例如Class和Struct。
 */
public class ClangDispatchVisitor extends CXCursorVisitor {

    private ClangContext context;

    private List<ParseExceptionElement> exceptionElements = new ArrayList<>();

    public ClangDispatchVisitor(ClangContext context) {
        this.context = context;
    }

    public ClangContext getContext() {
        return context;
    }

    protected AbstractNativeType dispatchParse(CXType type, CXClientData cxClientData) {

        CXType realType = ClangUtils.getRawType(type);
        String typeName = ClangUtils.getTypeName(realType,true);
        CXCursor cxCursor = LibClang.clang_getTypeDeclaration(type);

        dispatchParse(typeName, cxCursor, cxClientData);
        return context.getType(type);
    }

    protected int dispatchParse(String cursorName, CXCursor cxCursor, CXClientData cxClientData) {

        cursorName = ClangUtils.clearModifier(cursorName);
        String prefix = ClangUtils.getFullQualifiedPrefix(cxCursor);
        if (!prefix.isBlank() && !cursorName.startsWith(prefix)) {
            cursorName = prefix + "::" + cursorName;
        }

        // 处理C语言规则的header
        // C语言的类型体系：
        // 1. 各种基本类型，例如int，char等，这种不处理，内置在BasicType里面，
        //    解析到之后使用对应的单例对象
        // 2. 复合类型，Struct，Union。
        // 3. 指针类型，如果不能确定或者不应该确定类型，指针应该处理为void*，传递到其他语言的时候，使用long表达指针。
        //    函数指针也是指针类型，但是需要特殊处理。
        // 4. 数组类型，需要注意，不同长度的数组被视为不同的类型，例如int[10]和int[20]是两种类型。
        // 5. 声明的类型别名以及通过别名声明的匿名类型。
        // 6. 函数，也就是C语言的各类函数定义，函数不在本Visitor处理，因为函数是有作用范围的
        //    所以函数未来可能会包含在某些结构的作用域内，因此，它需要下放到Struct，Union或者未来的Class的Visitor中。
        //    无作用域或者全局的函数，在ClangGlobalVisitor中处理。

        if (cxCursor.kind() == LibClang.CXCursor_StructDecl) {
            // 结构体的处理，包括匿名结构体和命名结构体。
            doParseStruct(cursorName, cxClientData, cxCursor);
            return LibClang.CXChildVisit_Continue;
        } else if (cxCursor.kind() == LibClang.CXCursor_EnumDecl) {
            // 枚举类型的处理，包括匿名枚举和命名枚举。
            doParseEnum(cursorName, cxCursor,cxClientData);
            return LibClang.CXChildVisit_Continue;
        } else if (cxCursor.kind() == LibClang.CXCursor_TypedefDecl) {

            // 单独处理typedef，这里的typedef是在当前编译单元之外的，所以必须执行解析，
            // 无论这个typedef是否是一个匿名的typedef。
            CXType definedType = LibClang.clang_getCursorType(cxCursor);
            if (definedType.kind() == LibClang.CXType_Pointer) {

                int pointerLevel = 1;
                CXType pointee = LibClang.clang_getPointeeType(definedType);
                while (pointee.kind() == LibClang.CXType_Pointer) {
                    pointee = LibClang.clang_getPointeeType(pointee);
                    pointerLevel ++;
                }
                AbstractNativeType type = dispatchParse(pointee, cxClientData);
                if (type != null) {
                    NativePointerType pointerType = new NativePointerType(type,pointerLevel);
                    context.addDeclaredPointer(pointerType);
                    return LibClang.CXChildVisit_Continue;
                }
            } else if (definedType.kind() == LibClang.CXType_ConstantArray) {

                Stack<Long> sizeStack = new Stack<>();
                CXType element = LibClang.clang_getArrayElementType(definedType);
                long size = LibClang.clang_getArraySize(definedType);
                while (element.kind() == LibClang.CXType_ConstantArray) {
                    sizeStack.push(size);
                    size = LibClang.clang_getArraySize(element);
                    element = LibClang.clang_getArrayElementType(element);
                }

                AbstractNativeType type = dispatchParse(element, cxClientData);
                if (type != null) {
                    NativeArrayType arrayType = new NativeArrayType(type);
                    arrayType.setArraySize(size);
                    while (!sizeStack.isEmpty()) {
                        arrayType = new NativeArrayType(arrayType);
                        arrayType.setArraySize(sizeStack.pop());
                    }
                    context.addDeclaredArray(arrayType);
                }
                return LibClang.CXChildVisit_Continue;

            }

            CXType realType = ClangUtils.getRealType(definedType,false);
            CXCursor realTypeCursor = LibClang.clang_getTypeDeclaration(realType);
            if (LibClang.clang_Cursor_isAnonymous(realTypeCursor) == 1) {
                cursorName = ClangUtils.getTypeName(definedType, true);
            }
            return dispatchParse(cursorName, realTypeCursor, cxClientData);

        }

        CXString cursorKind = LibClang.clang_getCursorKindSpelling(cxCursor.kind());
        System.out.println("Cursor name: " + cursorName + " kind is " + ClangUtils.asString(cursorKind));
        ClangUtils.disposeStrings(cursorKind);

        return LibClang.CXChildVisit_Continue;
    }

    /**
     * 根据类型获取对应的NativeType，如果找不到，则返回null。
     * 如果在查找过程中发现类型缺失，应该通过dispatchParse进行解析。
     *
     * @param cxType
     * @param cxClientData
     * @return
     */
    protected AbstractNativeType getByType(CXType cxType, CXClientData cxClientData) {

        AbstractNativeType nativeType = null;

        if (ClangUtils.isPrimaryType(cxType)) {

            String typeName = ClangUtils.getModifierName(cxType);
            nativeType = BasicType.getByName(typeName);

        } else if (cxType.kind() == LibClang.CXType_Enum) {
            nativeType = context.getType(cxType);
            if (nativeType == null) {
                nativeType = dispatchParse(cxType,cxClientData);
            }
        } else if (cxType.kind() == LibClang.CXType_Elaborated) {

            CXType target = ClangUtils.getRawType(cxType);
            AbstractNativeType rawTargetType = getByType(target, cxClientData);
            if (rawTargetType == null) {
                rawTargetType = dispatchParse(target, cxClientData);
            }

            boolean isConst = LibClang.clang_isConstQualifiedType(cxType) == 1;
            boolean isVolatile = LibClang.clang_isVolatileQualifiedType(cxType) == 1;
            AbstractNativeType exist = context.getType(rawTargetType.getName(), isConst, isVolatile);
            if (exist != null) {
                return exist;
            }

            AbstractNativeType elaborated = rawTargetType.copy();
            elaborated.setVolatileType(isVolatile);
            elaborated.setConstType(isConst);
            context.addDeclaredType(elaborated);
            return elaborated;

        } else if (cxType.kind() == LibClang.CXType_Record) {

            nativeType = context.getType(cxType);
            if (nativeType == null) {
                nativeType = dispatchParse(cxType, cxClientData);
            }
            return nativeType;

        } else if (cxType.kind() == LibClang.CXType_Pointer) {

            // 这一部分的主要目的是查找，所以这里不应该直添加对应的Pointer类型
            // 所以解析并且确定Pointer结构后，使用解析得到的name获取类型才是正确的做法。
            // 如果找不到，则委托Visitor执行解析。
            CXType pointeeType = LibClang.clang_getPointeeType(cxType);
            int level = 1;
            while (pointeeType.kind() == LibClang.CXType_Pointer) {
                level ++;
                pointeeType = LibClang.clang_getPointeeType(pointeeType);
            }

            AbstractNativeType target = getByType(pointeeType, cxClientData);
            if (target != null) {

                if (target instanceof NativePointerType) {

                    NativePointerType pointer = (NativePointerType) target;
                    target = pointer.getPointeeType();
                    level = level + pointer.getPointerLevel();

                }

                NativePointerType pointerType = new NativePointerType(target, level);
                pointerType.setConstType(LibClang.clang_isConstQualifiedType(cxType) == 1);
                pointerType.setVolatileType(LibClang.clang_isVolatileQualifiedType(cxType) == 1);

                nativeType = context.getType(pointerType.getName(), pointerType.isConstType(), pointerType.isVolatileType());
                if (nativeType == null) {
                    context.addDeclaredPointer(pointerType);
                    nativeType = pointerType;
                }

            }


        } else if (cxType.kind() == LibClang.CXType_ConstantArray) {

            // 这里的主要目的是查找，所以解析Array类型后，不应该直接插入到Context中，
            // 而是使用构建的Array类型查找已存在的类型对象，如果找不到，则委托Visitor正式解析。

            Stack<Long> sizeStack = new Stack<>();
            CXType element = LibClang.clang_getArrayElementType(cxType);
            long size = LibClang.clang_getArraySize(cxType);
            while (element.kind() == LibClang.CXType_ConstantArray) {
                sizeStack.push(size);
                size = LibClang.clang_getArraySize(element);
                element = LibClang.clang_getArrayElementType(element);
            }

            AbstractNativeType type = getByType(element, cxClientData);
            if (type != null) {

                NativeArrayType arrayType = new NativeArrayType(type);
                arrayType.setArraySize(size);
                arrayType.setConstType(LibClang.clang_isConstQualifiedType(cxType) == 1);
                arrayType.setVolatileType(LibClang.clang_isVolatileQualifiedType(cxType) == 1);
                while (!sizeStack.isEmpty()) {
                    arrayType = new NativeArrayType(arrayType);
                    arrayType.setArraySize(sizeStack.pop());
                }
                nativeType = context.getType(arrayType.getName(),arrayType.isConstType(), arrayType.isVolatileType());
                if (nativeType == null) {
                    context.addDeclaredArray(arrayType);
                    nativeType = arrayType;
                }
            }

        } else if (cxType.kind() == LibClang.CXType_FunctionProto) {

            CXCursor cxCursor = LibClang.clang_getTypeDeclaration(cxType);
            ClangFuncParamVisitor funcVisitor = new ClangFuncParamVisitor(getContext());
            LibClang.clang_visitChildren(cxCursor, funcVisitor, cxClientData);
            funcVisitor.close();

            List<NativeFunctionParam> params = funcVisitor.getParams();
            int paramsCount = LibClang.clang_getNumArgTypes(cxType);
            if (paramsCount != params.size()) {
                params.clear();
                for (int index = 0; index < paramsCount; ++index) {

                    CXType paramType = LibClang.clang_getArgType(cxType, index);
                    AbstractNativeType nativeParamType = getByType(paramType, cxClientData);
                    if (nativeParamType == null) {
                        exceptionElements.add(ClangUtils.parseException("Can not parse param type", LibClang.clang_getTypeDeclaration(paramType)));
                        return null;
                    }
                    NativeFunctionParam param = new NativeFunctionParam();
                    param.setIndex(index);
                    param.setType(nativeParamType);
                    param.setName("arg" + index);
                    params.add(param);
                }
            }

            CXType returnType = LibClang.clang_getResultType(cxType);
            AbstractNativeType returnNativeType = getByType(returnType, cxClientData);

            if (returnNativeType == null) {
                exceptionElements.add(ClangUtils.parseException("Can not parse return type", LibClang.clang_getTypeDeclaration(returnType)));
                return null;
            }

            TypeParameterized returnParam = new TypeParameterized();
            returnParam.setName("result");
            returnParam.setType(returnNativeType);

            NativeFunction function = new NativeFunction("callback");
            function.setInstanceMethod(true);
            function.addFunctionParameterTypes(params);
            function.setReturnType(returnParam);
            function.setConstType(LibClang.clang_isConstQualifiedType(cxType) == 1);
            function.setVolatileType(LibClang.clang_isVolatileQualifiedType(cxType) == 1);
            function.setCallback(true);

            NativeFunction exists = getContext().getType(function.getName(), function.isConstType(), function.isVolatileType());
            if (exists == null) {
                getContext().addDeclaredFunction(function);
            } else {
                function = exists;
            }

            nativeType = function;

        }

        return nativeType;
    }

    private void doParseClass(String className, CXClientData clientData, CXCursor cxCursor) {


    }

    private void doParseStruct(String structName, CXClientData clientData, CXCursor cxCursor) {
        NativeStructType struct = new NativeStructType(structName);
        ClangStructVisitor visitor = new ClangStructVisitor(context, struct);
        context.addDeclaredStruct(struct);

        LibClang.clang_visitChildren(cxCursor, visitor, clientData);
        if (!visitor.getExceptions().isEmpty()) {
            exceptionElements.addAll(visitor.getExceptions());
            context.removeDeclaredStruct(struct);
        }

        System.out.println("Declared struct: " + structName);
        visitor.close();
    }


    private void doParseEnum(String enumName, CXCursor cxCursor, CXClientData clientData) {
        NativeEnumType enumType = new NativeEnumType(enumName);
        ClangEnumVisitor visitor = new ClangEnumVisitor(enumType);
        LibClang.clang_visitChildren(cxCursor, visitor, clientData);
        if (LibClang.clang_Cursor_isAnonymous(cxCursor) == 1) {
            // 展开为作用域内的常量
            List<NativeEnumField> enumValues = enumType.getEnumValues();
            for (NativeEnumField field : enumValues) {
                NativeConstant constant = new NativeConstant();
                constant.setName(field.getName());
                constant.setType(BasicType.LONG);
                context.addConstant(constant);
                System.out.println("Declared constant: " + constant.getName());
            }
            visitor.close();
        } else {
            // 展开为枚举类
            context.addDeclaredEnum(enumType);
            System.out.println("Declared enum: " + enumName);
            visitor.close();
        }
    }





}
