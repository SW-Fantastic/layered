package org.swdc.layered.def;

import org.swdc.layered.ExternalInvoker;
import org.swdc.layered.anno.ByteString;
import org.swdc.layered.anno.TypeMeta;
import org.swdc.layered.pointers.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

public class PlatformClosure extends OpaquePointer {

    private Method closureMethod;

    private WritableFunction callback;

    private Object delegate;

    private long closureAddress;

    private PlatformClosure(Object delegate, Method closureMethod) {
        this.closureMethod = closureMethod;
        this.delegate = delegate;
    }

    public Method getClosureMethod() {
        return closureMethod;
    }

    public boolean initCallback(Allocator allocator, WritableFunction callback, PlatformCallback cif) {
        if (getAddress() != 0 && closureAddress != 0) {
            if (this.callback == callback) {
                return true;
            }
            throw new IllegalStateException("Callback already initialized");
        }
        if (!doesMethodMatch(callback,closureMethod) || cif == null || cif.isNull()) {
            return false;
        }

        this.callback = callback;
        this.closureAddress = ExternalInvoker.createClosure(this,cif.getAddress());
        if (this.closureAddress == 0) {
            return false;
        }

        // 使用Closure的可执行地址作为本特殊指针的Address，该Address将作为参数传递给需要它的函数。
        long executableAddr = ExternalInvoker.getClosureFunctionAddr(closureAddress);
        if (executableAddr == 0) {
            this.closureAddress = 0;
            ExternalInvoker.freeClosure(closureAddress);
            return false;
        }
        initPointer(allocator, executableAddr,false);
        return true;
    }


    @Override
    protected void deAllocate() {
        if (closureAddress != 0) {
            ExternalInvoker.freeClosure(closureAddress);
            this.closureAddress = 0;
        }
    }

    private boolean doesTypeMatch(Class clazz, NativeDefinition nParam) {
        if (nParam == NativeDefinition.INT || nParam == NativeDefinition.UNSIGNED_INT) {
            return clazz == int.class || clazz == Integer.class;
        } else if (nParam == NativeDefinition.DOUBLE) {
            return clazz == double.class || clazz == Double.class;
        } else if (nParam == NativeDefinition.FLOAT) {
            return clazz == float.class || clazz == Float.class;
        } else if (nParam == NativeDefinition.LONG || nParam == NativeDefinition.UNSIGNED_LONG) {
            return clazz == long.class || clazz == Long.class;
        } else if (nParam == NativeDefinition.CHAR || nParam == NativeDefinition.UNSIGNED_CHAR) {
            return clazz == char.class || clazz == Character.class;
        } else if (nParam == NativeDefinition.SHORT || nParam == NativeDefinition.UNSIGNED_SHORT) {
            return clazz == short.class || clazz == Short.class;
        } else if (nParam == NativeDefinition.BOOL) {
            return clazz == boolean.class || clazz == Boolean.class;
        } else if (nParam == NativeDefinition.POINTER) {
            return OpaquePointer.class.isAssignableFrom(clazz);
        }
        return false;
    }

    private boolean doesMethodMatch(WritableFunction function, Method method) {
        if (method == null) {
            return false;
        }
        Parameter[] parameters = method.getParameters();
        List<WritableParameter> writableParameters = function.getParameters();
        if (parameters.length != writableParameters.size()) {
            return false;
        }
        for(int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            WritableParameter nParam = writableParameters.get(i);
            if (!doesTypeMatch(param.getType(), nParam.getType())) {
                return false;
            }
        }

        return doesTypeMatch(method.getReturnType(),function.getReturnType());
    }

    private void byCall(long returnAddr, long argsAddr) {

        if (closureMethod == null) {
            return;
        }

        TypeMeta retMeta = FFIUtils.extractMeta(closureMethod);

        List<WritableParameter> callBackParams = callback.getParameters();
        Parameter[] parameters = closureMethod.getParameters();
        Object[] args = new Object[parameters.length];
        AddressPointer pointer = AddressPointer.unmanaged(getAllocator(),argsAddr, callBackParams.size());
        for (int index = 0; index < parameters.length; index++) {
            Parameter parameter = parameters[index];
            TypeMeta typeDecl = FFIUtils.extractMeta(parameter);
            if (parameter.getType() == Integer.class || parameter.getType() == int.class) {
                IntPointer intPointer = (IntPointer) pointer.get(index,IntPointer.class);
                args[index] = intPointer.get(0);
                intPointer.free();
            } else if (parameter.getType() == Long.class || parameter.getType() == long.class) {
                LongPointer longPointer =(LongPointer) pointer.get(index,LongPointer.class);
                args[index] = longPointer.get(0);
                longPointer.free();
            } else if (parameter.getType() == Float.class || parameter.getType() == float.class) {
                FloatPointer floatPointer = (FloatPointer) pointer.get(index,FloatPointer.class);
                args[index] = floatPointer.get(0);
                floatPointer.free();
            } else if (parameter.getType() == Double.class || parameter.getType() == double.class) {
                DoublePointer doublePointer = (DoublePointer) pointer.get(index,DoublePointer.class);
                args[index] = doublePointer.get(0);
                doublePointer.free();
            } else if (parameter.getType() == Boolean.class || parameter.getType() == boolean.class) {
                BooleanPointer booleanPointer = (BooleanPointer) pointer.get(index,BooleanPointer.class);
                args[index] = booleanPointer.get(0);
                booleanPointer.free();
            } else if (parameter.getType() == String.class) {
                ByteString byteString = typeDecl.getCharset();
                BytePointer bytePointer = (BytePointer) pointer.get(index,BytePointer.class);
                args[index] = bytePointer.getString(byteString == null ? "UTF-8" : byteString.charset());
            } else if (OpaquePointer.class.isAssignableFrom(parameter.getType())) {
                AddressPointer pointerAddr =  (AddressPointer) pointer.get(index,AddressPointer.class);
                args[index] = pointerAddr.get(parameter.getType());
            } else {
                args[index] = null;
            }

        }
        try {

            Object result = closureMethod.invoke(delegate, args);

            NativeDefinition retType = callback.getReturnType();
            if (retType == NativeDefinition.VOID) {
                return;
            }

            Class ret = closureMethod.getReturnType();
            if (ret == Integer.class || ret == int.class) {
                IntPointer intPtr = IntPointer.unmanaged(getAllocator(),returnAddr, 1);
                intPtr.set(0, result == null ? 0 : (int)result);
                intPtr.free();
            } else if (ret == Long.class || ret == long.class) {
                LongPointer  longPtr = LongPointer.unmanaged(getAllocator(),returnAddr, 1);
                longPtr.set(0, result == null ? 0 : (long)result);
                longPtr.free();
            } else if (ret == Float.class || ret == float.class) {
                FloatPointer floatPointer = FloatPointer.unmanaged(getAllocator(),returnAddr, 1);
                floatPointer.set(0, result == null ? 0f : (float)result);
                floatPointer.free();
            } else if (ret == Double.class || ret == double.class) {
                DoublePointer doublePtr = DoublePointer.unmanaged(getAllocator(),returnAddr, 1);
                doublePtr.set(0, result == null ? 0d : (double)result);
                doublePtr.free();
            } else if (ret == Boolean.class || ret == boolean.class) {
                BooleanPointer booleanPtr = BooleanPointer.unmanaged(getAllocator(),returnAddr, 1);
                booleanPtr.set(0, result == null || (boolean)result);
                booleanPtr.free();
            } else if (ret == String.class) {
                String data = (String)result;
                if (result == null) {
                    data = "";
                }

                String charset;
                if (retMeta.getCharset() != null) {
                    charset = retMeta.getCharset().charset();
                } else {
                    charset = "UTF-8";
                }

                byte[] bytes = data.getBytes(charset);
                BytePointer bytePointer = BytePointer.unmanaged(getAllocator(),returnAddr, bytes.length);
                bytePointer.setBytes(bytes);
                bytePointer.free();
            } else if (OpaquePointer.class.isAssignableFrom(ret)) {

                OpaquePointer data = (OpaquePointer)result;
                AddressPointer addressPointer = AddressPointer.unmanaged(getAllocator(),returnAddr, 1);
                addressPointer.set(0,data);
                addressPointer.free();

            }

        } catch (Exception e) {

        }


    }

    public static <T> PlatformClosure create(T o, Class<T> clazz) {
        Annotation func = clazz.getAnnotation(FunctionalInterface.class);
        if (func == null) {
            throw new RuntimeException("Please use functional interface instead");
        }

        Method targetMethod = null;
        Method[] methods = clazz.getMethods();
        for (Method m : methods) {

            if (m.isBridge() || m.isDefault()) {
                continue;
            }
            if (m.getDeclaringClass() != clazz) {
                continue;
            }
            targetMethod = m;
            break;

        }

        if (targetMethod == null) {
            throw new RuntimeException("No method found for " + clazz.getName());
        }

        return new PlatformClosure(o,targetMethod);
    }



}
