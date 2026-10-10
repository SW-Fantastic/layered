package org.swdc.layered.calling;

import org.swdc.layered.MetaType;
import org.swdc.layered.pointers.*;
import org.swdc.layered.types.Const;
import org.swdc.layered.types.Symbol;
import org.swdc.layered.types.Unsigned;
import org.swdc.layered.types.Volatile;

import java.lang.reflect.*;
import java.security.InvalidParameterException;
import java.util.Stack;

public class TypeUtils {


    /**
     * 将 Java 的注解类型转换为对应的 C/C++ 类型符号字符串。
     *
     * <p>转换规则如下：
     * <ul>
     *     <li>对于普通 {@code Class} 类型：若标注了 {@code @Symbol}，直接使用其值（指针类型自动追加 {@code _ptr_}）；
     *     否则根据基本类型映射为 int/long/float/double/short/char/void，
     *     指针类型映射为 {@code xxx_ptr_}，并结合 {@code @Unsigned} 生成无符号类型名。</li>
     *     <li>对于 {@code PtrPointer} 参数化类型：递归解析泛型实参，逐层拼接 {@code ptr_} 以表示多级指针。</li>
     *     <li>{@code @Const} 与 {@code @Volatile} 会生成 {@code [CV]} 形式的前缀修饰符。</li>
     * </ul>
     *
     * @param aType 待转换的带注解的 Java 类型
     * @param parent 声明该类型的父级元素（如字段、方法参数），当 {@code aType} 上未直接标注相关注解时回退到该元素上查找，可为 {@code null}
     * @return 对应的 C/C++ 类型符号字符串；无法识别的类型将抛出异常。
     * @throws RuntimeException 当遇到不支持的类型时抛出
     */
    public static String getTypeSymbol(AnnotatedType aType, AnnotatedElement parent) {

        if(aType.getType() instanceof Class) {

            Class<?> type = (Class<?>)aType.getType();
            Symbol symbol = aType.getAnnotation(Symbol.class);
            if (symbol == null && parent != null) {
                symbol = parent.getAnnotation(Symbol.class);
            }

            if(symbol == null) {
                symbol = type.getAnnotation(Symbol.class);
            }

            String symbolName = "";
            if (symbol != null) {

                symbolName = symbol.value();
                if (OpaquePointer.class.isAssignableFrom(type)) {
                    symbolName += "_ptr_";
                }

            } else {

                Unsigned unsigned = aType.getAnnotation(Unsigned.class);
                if (unsigned == null && parent != null) {
                    unsigned = parent.getAnnotation(Unsigned.class);
                }
                boolean isUnsigned = (unsigned != null);

                if (type.equals(int.class) || type.equals(Integer.class) || type.equals(boolean.class) || type.equals(Boolean.class)) {
                    symbolName = (isUnsigned) ? "Uint" : "int";
                } else if (type.equals(long.class) || type.equals(Long.class)) {
                    symbolName = (isUnsigned) ? "Ulong" : "long";
                } else if (type.equals(float.class) || type.equals(Float.class)) {
                    symbolName = "float";
                } else if (type.equals(double.class) || type.equals(Double.class)) {
                    symbolName = "double";
                } else if (type.equals(short.class) || type.equals(Short.class)) {
                    symbolName = isUnsigned ? "Ushort" : "short";
                } else if (type.equals(char.class) || type.equals(Character.class)) {
                    symbolName = isUnsigned ? "Uchar" : "char";
                } else if (type.equals(void.class) || type.equals(Void.class)) {
                    symbolName = "void";
                } else if (OpaquePointer.class.isAssignableFrom(type)) {

                    if (type.equals(IntPointer.class)) {
                        symbolName = (isUnsigned) ? "Uint_ptr_" : "int_ptr_";
                    } else if (type.equals(LongPointer.class)) {
                        symbolName = (isUnsigned) ? "Ulong_ptr_" : "long_ptr_";
                    } else if (type.equals(FloatPointer.class)) {
                        symbolName = "float_ptr_";
                    } else if (type.equals(DoublePointer.class)) {
                        symbolName = "double_ptr_";
                    } else if (type.equals(ShortPointer.class)) {
                        symbolName = (isUnsigned) ? "Ushort_ptr_" :  "short_ptr_";
                    } else if (type.equals(BytePointer.class)) {
                        symbolName = isUnsigned ? "Uchar_ptr_" : "char_ptr_";
                    } else if (type.equals(OpaquePointer.class)) {
                        symbolName = "void_ptr_";
                    } else {
                        symbolName = type.getSimpleName() + "_ptr_";
                    }

                } else {
                    throw new RuntimeException("Unsupported type: " + type.getName());
                }

            }

            String modifiers = "";
            Const aConst = aType.getAnnotation(Const.class);
            if (aConst == null && parent != null) {
                aConst = parent.getAnnotation(Const.class);
            }

            if (aConst != null) {
                modifiers += "C";
            }

            Volatile aVolatile = aType.getAnnotation(Volatile.class);
            if (aVolatile == null && parent != null) {
                aVolatile = parent.getAnnotation(Volatile.class);
            }
            if (aVolatile != null) {
                modifiers += "V";
            }

            if (!modifiers.isEmpty()) {
                symbolName = "[" + modifiers + "]" + symbolName;
            }

            return symbolName;

        } else if (aType.getType() instanceof ParameterizedType) {

            AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) aType;
            ParameterizedType rawParamType = (ParameterizedType) parameterizedType.getType();
            Class<?> rawType = (Class<?>)rawParamType.getRawType();

            if (rawType == PtrPointer.class) {

                Stack<AnnotatedType> ptrStack = new Stack<>();
                AnnotatedType argType = parameterizedType.getAnnotatedActualTypeArguments()[0];
                Type genericArgType = argType.getType();
                while (genericArgType instanceof ParameterizedType) {
                    ptrStack.push(argType);
                    AnnotatedParameterizedType annoParamArg = (AnnotatedParameterizedType)argType;
                    argType = annoParamArg.getAnnotatedActualTypeArguments()[0];
                    genericArgType = argType.getType();
                }

                String symbolName = getTypeSymbol(argType, null);
                String ptrSymbol = "";
                while (!ptrStack.isEmpty()) {
                    AnnotatedType ptrType = ptrStack.pop();
                    String ptrModifiers = "";
                    Const ptrConst = ptrType.getAnnotation(Const.class);
                    if (ptrConst != null) {
                        ptrModifiers += "C";
                    }
                    Volatile ptrVolatile = ptrType.getAnnotation(Volatile.class);
                    if (ptrVolatile != null) {
                        ptrModifiers += "V";
                    }
                    if (!ptrModifiers.isBlank()) {
                        ptrSymbol = ptrSymbol + "ptr_" + "[" + ptrModifiers + "]";
                    } else {
                        ptrSymbol += "ptr_";
                    }
                }

                if (symbolName.endsWith("_")) {
                    return symbolName + ptrSymbol;
                }
                return symbolName + "_" + ptrSymbol;

            }

        }

        throw new RuntimeException("Unsupported type: " + aType.getType().toString());
    }
    
    /**
     * 将 MetaType 元类型映射为对应的 InvokerType 调用器类型。
     *
     * <p>用于 FFI 调用时根据 Java 类型选择正确的调用方式。
     *
     * @param type 元类型，不能为 null
     * @return 对应的 InvokerType 调用器类型
     * @throws InvalidParameterException 当 type 为 null 时抛出
     * @throws RuntimeException 当遇到未知的 MetaType 时抛出
     */
    public static InvokerType getInvokerType(MetaType type) {
        if (type == null) {
            throw new InvalidParameterException("type is null");
        }
        if (type == MetaType.INT) {
            return InvokerType.LAYER_INT;
        } else if (type == MetaType.LONG) {
            return InvokerType.LAYER_LONG;
        } else if (type == MetaType.FLOAT) {
            return InvokerType.LAYER_FLOAT;
        } else if (type == MetaType.DOUBLE) {
            return InvokerType.LAYER_DOUBLE;
        } else if (type == MetaType.INTPTR_T) {
            return InvokerType.LAYER_ADDRESS;
        } else if (type == MetaType.VOID) {
            return InvokerType.LAYER_VOID;
        } else if (type == MetaType.SIZE_T) {
            return InvokerType.LAYER_LONG;
        } else {
            throw new RuntimeException("Unknown meta type: " + type);
        }
    }

}
