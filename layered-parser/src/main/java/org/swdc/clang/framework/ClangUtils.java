package org.swdc.clang.framework;

import org.bytedeco.javacpp.BytePointer;
import org.swdc.clang.framework.def.*;
import org.swdc.libclang.core.CXCursor;
import org.swdc.libclang.core.CXType;
import org.swdc.libclang.core.LibClang;
import org.swdc.libclang.core.io.CXFile;
import org.swdc.libclang.core.io.CXSourceLocation;
import org.swdc.libclang.core.io.CXString;
import org.swdc.libclang.core.io.ClangIO;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClangUtils {

    /**
     * 读取CXString的字符串值。
     * @param string CXString对象。
     * @return 字符串值。
     */
    public static String asString(CXString string) {
        BytePointer pointer = ClangIO.clang_getCString(string);
        byte[] data = pointer.getStringBytes();
        if (data == null || data.length == 0) {
            return null;
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * 读取CXString的字符串值，并释放资源。
     * @param string CXString对象。
     * @return 字符串值。
     */
    public static String readString(CXString string) {
        try {
            if (string == null ||string.isNull()) {
                return null;
            }
            return asString(string);
        } finally {
            if (string != null && !string.isNull()) {
                ClangIO.clang_disposeString(string);
            }
        }
    }

    /**
     * 销毁CXString对象。
     * @param strings CXString对象数组。
     */
    public static void disposeStrings(CXString... strings) {
        for (CXString string : strings) {
            if (string == null || string.isNull()) {
                continue;
            }
            ClangIO.clang_disposeString(string);
        }
    }


    /**
     * 是否为不可变的常量。
     * @param type CXType对象。
     * @return 是否为不可变的常量。
     */
    public static boolean isConst(CXType type) {
        type = getRawType(type);
        while (type.kind() == LibClang.CXType_Pointer) {
            type = LibClang.clang_getPointeeType(type);
        }
        return LibClang.clang_isConstQualifiedType(type) == 1;
    }

    private static boolean isPlatformDependentType(CXType type) {
        String name = readString(LibClang.clang_getTypeSpelling(type));
        if (name != null && !name.isBlank()) {
            name = name.toLowerCase();
            return name.equals("size_t") ||
                    name.equals("time_t") ||
                    name.equals("ssize_t");
        }
        return false;
    }

    /**
     * 解析原始类型，不穿透指针。
     * @param type CXType对象。
     * @return 原始类型。
     */
    public static CXType getRawType(CXType type) {
        while (type.kind() == LibClang.CXType_Typedef || type.kind() == LibClang.CXType_Elaborated) {
            // 解掉typedef类型，获取实际的类型
            if (type.kind() == LibClang.CXType_Typedef) {

                if (isPlatformDependentType(type)) {
                    return type;
                }

                CXCursor cursor = LibClang.clang_getTypeDeclaration(type);
                type = LibClang.clang_getTypedefDeclUnderlyingType(cursor);
            } else if (type.kind() == LibClang.CXType_Elaborated) {
                type = LibClang.clang_Type_getNamedType(type);
            }
        }
        return type;
    }

    /**
     * 获取真实的类型，穿透别名和指针。
     *
     * @param type     CXType对象。
     * @param keepName 是否保留匿名类型的名称，如果为true，则至少保留一个可用的typedef别名，
     *                 防止出现匿名struct解析到最后完全没有name可用的问题。
     * @return 真实的类型。
     */
    public static CXType getRealType(CXType type, boolean keepName) {

        /*if (isPrimaryType(type) && !isPlatformDependentType(type)) {
            return LibClang.clang_getCanonicalType(type);
        }*/

        List<Integer> aliasKind = Arrays.asList(
                LibClang.CXType_Typedef,
                LibClang.CXType_Pointer,
                LibClang.CXType_Elaborated
        );

        while (aliasKind.contains(type.kind())) {
            // 解掉typedef类型，获取实际的类型
            if (type.kind() == LibClang.CXType_Pointer) {
                // 指针类型，获取指向的类型即可。
                type = LibClang.clang_getPointeeType(type);
            } else if (type.kind() == LibClang.CXType_Elaborated ) {
                type = LibClang.clang_Type_getNamedType(type);
            } else {
                if (isPlatformDependentType(type)) {
                    return type;
                }
                CXCursor cursor = LibClang.clang_getTypeDeclaration(type);
                if (keepName && LibClang.clang_Cursor_isAnonymous(cursor) == 1) {
                    // 内层的类型是匿名的，它只有一个typedef名称，不要继续解构它
                    // 至少保证一个它存在一个可用的name。
                    return type;
                }
                type = LibClang.clang_getTypedefDeclUnderlyingType(cursor);
            }
        }

        return type;

    }



    /**
     * 判断是否为函数类型。
     * @param type CXType对象。
     * @return 是否为函数类型。
     */
    public static boolean isFunction(CXType type) {
        type = getRealType(type,false);
        return type.kind() == LibClang.CXType_FunctionProto || type.kind() == LibClang.CXType_FunctionNoProto;
    }

    /**
     * 获取类型的名称。
     *
     * @param type       CXType对象。
     * @param qualified  是否需要全限定名，如果为true，则返回完整的namespace路径。
     * @return 类型的完整名称。
     */
    public static String getTypeName(CXType type, boolean qualified) {
        CXType realType = getRealType(type,true);
        String prefix = getFullQualifiedPrefix(realType);
        String name = readString(LibClang.clang_getTypeSpelling(type));
        if (name == null) {
            name = "";
        }
        String fullPrefix = qualified && !prefix.isBlank() ? prefix + "::" : "";
        if (name.contains("std") || fullPrefix.contains("std")) {
            return fullPrefix + name;
        } else {
            name = readString(LibClang.clang_getTypeSpelling(realType));
            return fullPrefix + name;
        }
    }

    /**
     * 获取Cursor的完整限定前缀，例如：
     * <pre>
     * class A {
     * public:
     *     class B {
     *
     *     }
     * }
     * </pre>
     * 访问到Class B时，返回 "A"，对于namespace，struct等都像这样处理，
     * 在类型之前添加这样的前缀可以确保作用域的正确性
     * @param cxCursor Cursor。
     * @return 该Cursor的全限定前缀，如果不处于任何作用域中（或者说处于全局范围），则返回空字符串。
     */
    public static String getFullQualifiedPrefix(CXCursor cxCursor) {

        StringBuilder qualifiedPrefix = new StringBuilder();
        CXCursor parent = LibClang.clang_getCursorSemanticParent(cxCursor);

        while (parent.kind() != LibClang.CXCursor_TranslationUnit) {
            // 最外层的事TranslationUnit，它指的是全局作用域，
            // 如果抵达了全局作用域，则跳出循环。
            if (LibClang.clang_Cursor_isAnonymous(parent) == 1) {
                // 作用域是匿名的，例如匿名namespace或者struct等
                // 匿名作用域不需要出现在全限定名中。
                // 继续向上查找父作用域
                parent = LibClang.clang_getCursorSemanticParent(parent);
                continue;
            }
            while (parent.kind() == LibClang.CXCursor_NamespaceAlias || parent.kind() == LibClang.CXCursor_TypeAliasDecl) {
                // 如果是别名，则获取其指向的真实类型
                parent = LibClang.clang_getCursorReferenced(parent);
            }
            // 获取作用域名称，追加到qualifiedPrefix的前面
            CXString name = LibClang.clang_getCursorSpelling(parent);
            String parentName = readString(name);
            if (parentName == null) {
                break;
            }
            if (!parentName.isBlank()) {
                if (qualifiedPrefix.length() > 0) {
                    qualifiedPrefix.insert(0, "::");
                }
                qualifiedPrefix.insert(0, parentName);
            }
            parent = LibClang.clang_getCursorSemanticParent(parent);
        }

        return qualifiedPrefix.toString();

    }

    /**
     * 获取类型的完整限定前缀。
     * @param type CXType对象。
     * @return 类型的完整限定前缀。
     */
    public static String getFullQualifiedPrefix(CXType type) {
        CXCursor cursor = LibClang.clang_getTypeDeclaration(type);
        return getFullQualifiedPrefix(cursor);
    }


    public static String getModifierName( String typeName, boolean isConst, boolean isVolatile ) {

        typeName = ClangUtils.clearModifier(typeName);

        boolean first = true;
        String fqn = typeName;
        if (isConst) {
            fqn = fqn + "?const";
            first = false;
        }
        if (isVolatile) {
            fqn = fqn + (first ? "?" : "&") + "volatile";
            first = false;
        }

        return fqn;
    }

    public static String getModifierName(CXType type) {

        String typeName = getTypeName(type,true);
        return getModifierName(typeName,
                LibClang.clang_isConstQualifiedType(type) == 1,
                LibClang.clang_isVolatileQualifiedType(type) == 1
        );
    }

    /**
     * 清除修饰符。
     * @param typeName 类型名称。
     * @return 清除修饰符后的类型名称。
     */
    public static String clearModifier(String typeName) {
        return typeName
                .replace("const", "")
                .replace("struct", "")
                .replace("union", "")
                .replace("*", "")
                .replace("enum", "")
                .trim();
    }

    public static boolean isPrimaryType(CXType type) {
        type = LibClang.clang_getCanonicalType(type);
        return Arrays.asList(
                LibClang.CXType_Void,
                LibClang.CXType_Bool,

                LibClang.CXType_Char_U,
                LibClang.CXType_Char_S,
                LibClang.CXType_SChar,
                LibClang.CXType_WChar,
                LibClang.CXType_UChar,
                LibClang.CXType_Char16,
                LibClang.CXType_Char32,

                LibClang.CXType_UShort,
                LibClang.CXType_UInt,
                LibClang.CXType_UInt128,
                LibClang.CXType_ULong,
                LibClang.CXType_ULongLong,

                LibClang.CXType_Int,
                LibClang.CXType_Long,
                LibClang.CXType_LongLong,
                LibClang.CXType_Short,
                LibClang.CXType_Float,
                LibClang.CXType_Double,
                LibClang.CXType_LongDouble
        ).contains(type.kind());
    }

    public static ParseExceptionElement parseException(String message, CXCursor cxCursor) {

        CXSourceLocation location = LibClang.clang_getCursorLocation(cxCursor);
        CXFile file = new CXFile();
        int[] line = new int[1];
        int[] column = new int[1];
        int[] offset = new int[1];
        ClangIO.clang_getSpellingLocation(location, file, line, column, offset);
        CXString currentPath = ClangIO.clang_File_tryGetRealPathName(file);
        if (currentPath == null || currentPath.isNull()) {
            return null;
        }

        String filePath = ClangUtils.asString(currentPath);
        ClangUtils.disposeStrings(currentPath);
        String msg = String.format("File: %s line: %d offset:%d", filePath, line[0], column[0]);
        return new ParseExceptionElement(msg, message);

    }

    public static String generateMangled(List<? extends TypeParameterized> params, boolean simple) {
        StringBuilder result = new StringBuilder();
        for (TypeParameterized param: params) {
            if (param.getType() instanceof BasicType) {
                // 基本类型，直接使用提供的flag
                BasicType basic = (BasicType) param.getType();
                result.append(simple ? basic.getSimpleMangledFlag() : basic.getMangledFlag());
            } else if (param.getType() instanceof NativeEnumType) {
                // 枚举类型，使用枚举类型的序列编号
                result.append("I");
            } else if (param.getType() instanceof NativeArrayType) {

                NativeArrayType array = (NativeArrayType) param.getType();
                result.append("A").append(array.getArraySize());
                while (array.getElementType() instanceof NativeArrayType) {
                    array = (NativeArrayType) array.getElementType();
                    result.append("A").append(array.getArraySize());
                }

                TypeParameterized elementType = new TypeParameterized();
                elementType.setType(param.getType());
                result.append(generateMangled(List.of(elementType), simple));

            } else if (param.getType() instanceof NativePointerType) {

                NativePointerType pointerType = (NativePointerType) param.getType();
                AbstractNativeType type = pointerType.getPointeeType();
                if (type instanceof NativeFunction || type instanceof NativeStructType || type instanceof NativeArrayType) {
                    // 这三个本身对外就表现为指针，不需要在多加一层。
                    result.append("V").append("p".repeat(pointerType.getPointerLevel()));
                    continue;
                }

                TypeParameterized typeParameterized = new TypeParameterized();
                typeParameterized.setType(pointerType.getPointeeType());
                String name = generateMangled(Collections.singletonList(typeParameterized),simple) + "p";
                result.append(name);

                if (!simple) {
                    String cv = "";
                    if (pointerType.isConstType()) {
                        cv += "C";
                    }
                    if (pointerType.isVolatileType()) {
                        cv += "V";
                    }
                    if (!cv.isEmpty()) {
                        result.append("[").append(cv).append("]");
                    }
                }


            } else {
                result.append("Vp");
            }
        }
        return result.toString();
    }

}
