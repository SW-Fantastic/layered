package org.swdc.layered.calling;


import org.swdc.layered.pointers.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LayerInvokeHandler implements InvocationHandler {

    private LayerInvoker invoker;

    public LayerInvokeHandler(LayerInvoker invoker) {
        this.invoker = invoker;
    }

    /**
     * 代理调用入口，负责将 Java 方法调用转发到底层动态库并转换返回值。
     *
     * 通过 CIF 处理器将入参转换为原生指针后执行调用，再根据方法声明的返回类型，
     * 将调用结果指针解引用为对应的 Java 基本类型或包装类型；对于指针类型，
     * 则通过 UniquePointer 构造函数构造包装对象，最后释放临时结果指针。
     *
     * @param proxy 代理对象本身
     * @param method 被调用的方法，用于获取对应的 CIF 处理器及返回类型
     * @param args 方法调用入参
     * @return 转换后的返回值，void 方法返回 null
     * @throws Throwable 当返回值类型无法转换时抛出异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        OpaquePointer result = null;
        LayerBuffer buffer = new LayerBuffer(invoker.getAllocator(), args == null ? 0 : args.length);

        try {

            LayerCIFHandler handler = invoker.getCIFHandler(method);
            handler.transformParameters(buffer, args);
            result = handler.call(buffer);

            Class<?> returnType = method.getReturnType();
            if (returnType == void.class || returnType == Void.class) {
                result.close();
                return null;
            } else if(returnType == int.class || returnType == Integer.class) {
                IntPointer intPointer = (IntPointer)result;
                int res = intPointer.get(0);
                result.close();
                return res;
            } else if(returnType == long.class || returnType == Long.class) {
                LongPointer longPointer = (LongPointer)result;
                long res = longPointer.get(0);
                result.close();
                return res;
            } else if (returnType == float.class || returnType == Float.class) {
                FloatPointer floatPointer = (FloatPointer)result;
                float res = floatPointer.get(0);
                result.close();
                return res;
            } else if (returnType == double.class || returnType == Double.class) {
                DoublePointer  doublePointer = (DoublePointer)result;
                double res = doublePointer.get(0);
                result.close();
                return res;
            } else if (returnType == short.class || returnType == Short.class) {
                ShortPointer shortPointer = (ShortPointer)result;
                short res = shortPointer.get(0);
                result.close();
                return res;
            } else if (returnType == boolean.class || returnType == Boolean.class) {

                if (result instanceof IntPointer) {
                    IntPointer intPointer = (IntPointer)result;
                    int res = intPointer.get(0);
                    result.close();
                    return (res != 0);
                } else if (result instanceof BytePointer) {
                    BytePointer bytePointer = (BytePointer) result;
                    byte res = bytePointer.get(0);
                    result.close();
                    return (res != 0);
                } else if (result instanceof ShortPointer) {
                    ShortPointer intPointer = (ShortPointer)result;
                    int res = intPointer.get(0);
                    result.close();
                    return (res != 0);
                }

                result.close();
                throw new RuntimeException("Can not cast to boolean return type");

            } else if (returnType == char.class || returnType == Character.class) {
                BytePointer bytePointer = (BytePointer)result;
                byte res = bytePointer.get(0);
                result.close();
                return (char)res;
            } else if (returnType == byte.class || returnType == Byte.class) {
                BytePointer bytePointer = (BytePointer)result;
                byte res = bytePointer.get(0);
                result.close();
                return res;
            } else if (OpaquePointer.class.isAssignableFrom(returnType)) {
                try {

                    PtrPointer ptrPointer = new PtrPointer(buffer.getResultBuf().offset(0).move());
                    Object resultPtr = ptrPointer.getAddress(0, returnType);
                    ptrPointer.close();
                    return resultPtr;

                } catch (Exception e) {
                    throw new InvocationTargetException(e, "Can not cast to target return type " +  returnType.getName() + ", because no UniquePointer constructor found.");
                }

            }
            return null;

        } catch (Throwable throwable) {

            if (result != null) {
                result.close();
            }
            throw throwable;

        } finally {
            buffer.close();
        }

    }

}
