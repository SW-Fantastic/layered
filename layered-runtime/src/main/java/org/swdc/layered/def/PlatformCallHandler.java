package org.swdc.layered.def;

import org.swdc.layered.anno.*;
import org.swdc.layered.pointers.*;

import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PlatformCallHandler implements InvocationHandler {

    private PlatformModule module;

    private ConcurrentHashMap<Method, PlatformCallback> initializedCallbacks = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Method, PlatformCall> initializedCalls = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Method, WritableFunction> calls = new ConcurrentHashMap<>();

    public PlatformCallHandler(PlatformModule module) {
        this.module = module;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        PlatformCall target = null;
        if (initializedCalls.containsKey(method)) {
            target = initializedCalls.get(method);
        } else {
            String methodSymbol = generateCallSymbol(method);
            WritableFunction function = module.getSymbolTable().get(methodSymbol);
            if (function == null) {
                throw new LinkageError("Cannot find function " + methodSymbol);
            }
            if (function.getSymbolName() == null || function.getSymbolName().isEmpty()) {
                throw new LinkageError("Cannot find function " + methodSymbol + " no symbol name");
            }

            long symbolAddr = module.lookup(function.getSymbolName());
            if (symbolAddr == 0) {
                throw new LinkageError("Cannot find function " + function.getSymbolName() + " no such export function");
            }

            target = new PlatformCall(module.getAllocator(), function, symbolAddr);
            initializedCalls.put(method, target);
            calls.put(method, function);
        }

        if (args == null) {
            args = new Object[0];
        }

        List<OpaquePointer> autoAllocated = new ArrayList<>();
        Allocator allocator = module.getAllocator();
        OpaquePointer[] targetArgs = new OpaquePointer[args.length];
        for (int i = 0; i < args.length; i++) {

            if (args[i] instanceof OpaquePointer) {
                targetArgs[i] = (OpaquePointer)args[i];
                if (args[i] instanceof PlatformClosure) {
                    // 这是一个Closure，进行额外的初始化处理。
                    WritableFunction function = calls.get(method);
                    WritableParameter parameter = function.getParameters().get(i);
                    WritableFunction callback = parameter.getCallback();

                    PlatformClosure closure = (PlatformClosure)args[i];
                    PlatformCallback nCallback = null;
                    if (initializedCallbacks.contains(method)) {
                        nCallback = initializedCallbacks.get(method);
                    } else {
                        Method targetCallback = closure.getClosureMethod();
                        PlatformCallback platformCallback = new PlatformCallback(allocator, callback);
                        initializedCallbacks.put(targetCallback, platformCallback);
                        nCallback = platformCallback;
                    }

                    if(!closure.initCallback(allocator,callback,nCallback)) {
                        throw new IllegalStateException("Cannot initialize callback, function does not matched");
                    }
                }
            } else {

                OpaquePointer autoPtr = null;
                if (args[i] == null) {
                    autoPtr = allocator.allocateByType(method.getParameterTypes()[i],1);
                } else {
                    autoPtr = allocateByObject(allocator, args[i]);
                }

                if (autoPtr == null) {
                    autoPtr = allocator.allocateNull();
                }
                autoAllocated.add(autoPtr);
                targetArgs[i] = autoPtr;

            }
        }

        try {
            if (method.getReturnType().isPrimitive()) {
                OpaquePointer autoPtr = allocator.allocateByType(method.getReturnType(),1);
                autoAllocated.add(autoPtr);
                if (method.getReturnType() != void.class && method.getReturnType() != Void.class) {
                    if ((autoPtr == null || autoPtr.isNull())) {
                        throw new RuntimeException("Cannot allocate " + method.getReturnType().getName() + " as a primitive type");
                    }
                    if (method.getReturnType() == Integer.class ||  method.getReturnType() == int.class) {
                        IntPointer intPointer = (IntPointer)autoPtr;
                        target.call(intPointer,targetArgs);
                        return intPointer.get(0);
                    }
                    if (method.getReturnType() == Long.class ||  method.getReturnType() == long.class) {
                        LongPointer longPointer = (LongPointer)autoPtr;
                        target.call(longPointer,targetArgs);
                        return longPointer.get(0);
                    }
                    if (method.getReturnType() == Double.class ||  method.getReturnType() == double.class) {
                        DoublePointer doublePointer = (DoublePointer)autoPtr;
                        target.call(doublePointer,targetArgs);
                        return doublePointer.get(0);
                    }
                    if (method.getReturnType() == Boolean.class ||  method.getReturnType() == boolean.class) {
                        BooleanPointer booleanPointer = (BooleanPointer)autoPtr;
                        target.call(booleanPointer,targetArgs);
                        return booleanPointer.get(0);
                    }
                    if (method.getReturnType() == Short.class ||  method.getReturnType() == short.class) {
                        ShortPointer shortPointer = (ShortPointer)autoPtr;
                        target.call(shortPointer,targetArgs);
                        return shortPointer.get(0);
                    }
                    if (method.getReturnType() == Byte.class ||  method.getReturnType() == byte.class) {
                        BytePointer bytePointer = (BytePointer)autoPtr;
                        target.call(bytePointer,targetArgs);
                        return bytePointer.getBytes(1)[0];
                    }
                    if (method.getReturnType() == Float.class ||  method.getReturnType() == float.class) {
                        FloatPointer floatPointer = (FloatPointer)autoPtr;
                        target.call(floatPointer,targetArgs);
                        return floatPointer.get(0);
                    }

                    throw new IllegalArgumentException("Cannot allocate " + method.getReturnType().getName() + " as a primitive type");
                } else {

                    AddressPointer pointer = allocator.allocateAddress(1);
                    autoAllocated.add(pointer);
                    target.call(pointer, targetArgs);

                    return null;
                }
            } else if (method.getReturnType() == String.class) {

                AddressPointer<BytePointer> stringPtr = allocator.allocateAddress(1);
                autoAllocated.add(stringPtr);
                target.call(stringPtr,targetArgs);
                BytePointer result = stringPtr.get(0, BytePointer.class);
                if (result == null || result.isNull()) {
                    return null;
                }
                return result.getString(StandardCharsets.UTF_8);

            } else if (OpaquePointer.class.isAssignableFrom(method.getReturnType())) {

                AddressPointer targetPtr = allocator.allocateAddress(1);
                autoAllocated.add(targetPtr);
                target.call(targetPtr,targetArgs);
                return targetPtr.get(0, method.getReturnType());

            } else {
                throw new IllegalStateException("invalid return type found.");
            }

        } finally {
            for (OpaquePointer autoPtr : autoAllocated) {
                autoPtr.free();
            }
        }

    }

    private OpaquePointer allocateByObject(Allocator allocator, Object arg) {
        if (arg.getClass() == Integer.class) {

            IntPointer intArg = allocator.allocateInt(1);
            intArg.set(0, (Integer)arg);
            return intArg;

        } else if (arg.getClass() == Short.class) {

            ShortPointer shortArg = allocator.allocateShort(1);
            shortArg.set(0, (Short)arg);
            return shortArg;

        } else if (arg.getClass() == Long.class) {

            LongPointer longArg = allocator.allocateLong(1);
            longArg.set(0, (Long)arg);
            return longArg;

        } else if (arg.getClass() == Float.class) {

            FloatPointer floatArg = allocator.allocateFloat(1);
            floatArg.set(0, (Float)arg);
            return floatArg;

        } else if (arg.getClass() == Double.class) {

            DoublePointer doubleArg = allocator.allocateDouble(1);
            doubleArg.set(0, (Double)arg);
            return doubleArg;

        } else if (arg.getClass() == String.class) {

            return allocator.allocateByte(arg.toString());

        } else if (arg.getClass() == Boolean.class) {

            BooleanPointer booleanArg = allocator.allocateBoolean(1);
            booleanArg.set(0, (Boolean)arg);
            return booleanArg;

        }

        return null;
    }


    private String generateCallSymbol(Method method) {

        TypeMeta typeMeta = FFIUtils.extractMeta(method);
        Class returnTypeClazz = method.getReturnType();
        if (typeMeta.getCast() != null) {
            returnTypeClazz = typeMeta.getCast().value();
        }

        String paramTypes = "";
        String returnType = getMangedFromType(returnTypeClazz,typeMeta);
        for (Parameter parameter : method.getParameters()) {
            TypeMeta pTypeMeta = FFIUtils.extractMeta(parameter);
            Class paramType = parameter.getType();
            if (pTypeMeta.getCast() != null) {
                paramType = pTypeMeta.getCast().value();
            }
            paramTypes = paramTypes + getMangedFromType(paramType,pTypeMeta);
        }
        String symbolName = method.getName();
        Symbol symbol = method.getAnnotation(Symbol.class);
        if (symbol != null && !symbol.value().isBlank()) {
            symbolName = symbol.value();
        }

        SymbolCtor symbolFactory = method.getAnnotation(SymbolCtor.class);
        if (symbolFactory != null && !symbolFactory.value().isBlank()) {
            symbolName = symbolFactory.value() + "__new";
        }

        SymbolDtor symbolDtor = method.getAnnotation(SymbolDtor.class);
        if (symbolDtor != null && !symbolDtor.value().isBlank()) {
            symbolName = symbolDtor.value() + "__delete";
        }

        SymbolGetter getter = method.getAnnotation(SymbolGetter.class);
        if (getter != null && !getter.type().isBlank() && !getter.field().isBlank()) {
            symbolName = getter.type() + "_get_" + getter.field();
        }

        SymbolSetter setter = method.getAnnotation(SymbolSetter.class);
        if (setter != null && !setter.type().isBlank() && !setter.field().isBlank()) {
            symbolName = setter.type() + "_set_" + setter.field();
        }

        return "(" + returnType + ")" + symbolName + "@" + paramTypes;
    }

    public String getMangedFromType(Class type, TypeMeta typeDecl) {

        String result = "";
        if (type == int.class || type == Integer.class) {
            if (typeDecl != null && typeDecl.getUnsigned() != null) {
                result = "Ui";
            } else {
                result = "I";
            }
        } else if (type == long.class || type == Long.class) {
            if (typeDecl != null) {

                if (typeDecl.getUnsigned() != null) {
                    result = "Ul";
                } else {
                    result = "L";
                }

                if (typeDecl.getCastValue() != null) {
                    CastValue castValue = typeDecl.getCastValue();
                    if (castValue.value() == PlatformType.SIZE_T) {
                        result = "Sz";
                    } else if (castValue.value() == PlatformType.SSIZE_T) {
                        result = "Ssz";
                    } else if (castValue.value() == PlatformType.TIME_T) {
                        result = "Ts";
                    }
                }
            } else {
                result = "L";
            }

        } else if (type == short.class || type == Short.class) {
            if (typeDecl != null) {
                result = "Us";
            } else {
                result = "S";
            }
        } else if (type == void.class || type == Void.class) {
            result = "V";
        } else if (type == float.class || type == Float.class) {
            result = "F";
        } else if (type == double.class || type == Double.class) {
            result = "D";
        } else if (type == boolean.class || type == Boolean.class) {
            result = "B";
        } else if (type == char.class || type == Character.class) {
            result = "C";
        } else if (typeDecl != null && typeDecl.getArray() != null) {
            ConstArray array =  typeDecl.getArray();
            result = "A" + array.length() + getMangedFromType(type.getComponentType(), typeDecl);
        } else if (SeekablePointer.class.isAssignableFrom(type)) {
            if (type == SeekablePointer.class) {
                throw new IllegalStateException("Invalid type: SeekablePointer");
            }
            Class current = type;
            while (current.getSuperclass() != SeekablePointer.class) {
                current = current.getSuperclass();
            }

            int pointerLevel = 1;
            while (current == AddressPointer.class) {
                ParameterizedType target = (ParameterizedType) current.getGenericSuperclass();
                current = (Class) target.getActualTypeArguments()[0];
                pointerLevel++;
            }

            ParameterizedType target = (ParameterizedType) current.getGenericSuperclass();
            Class pointee = (Class) target.getActualTypeArguments()[0];
            if (pointee == OpaquePointer.class) {
                pointerLevel --;
            }
            result = getMangedFromType(pointee,typeDecl) + "p".repeat(pointerLevel);

        } else if (OpaquePointer.class.isAssignableFrom(type)) {
            result = "Vp";
        } else if (String.class == type) {
            return "Cp";
        }
        if (!result.isBlank()) {
            return result;
        }
        throw new IllegalStateException("Can not generate call symbol for type: " + type);
    }


    public void free() {
        for (PlatformCall call: initializedCalls.values()) {
            call.free();
        }
        initializedCalls.clear();
    }

}
