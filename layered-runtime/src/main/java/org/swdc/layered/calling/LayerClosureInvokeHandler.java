package org.swdc.layered.calling;

import org.swdc.layered.MemoryManager;
import org.swdc.layered.pointers.*;
import org.swdc.layered.types.Closure;
import org.swdc.layered.types.SafeLong;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 封装了闭包调用逻辑的动态代理处理器,
 * 用以支持主动调用Native的函数指针。
 */
public class LayerClosureInvokeHandler implements InvocationHandler {

    private LayerInvoker invoker;

    private LayerClosure closure;

    public LayerClosureInvokeHandler(LayerInvoker invoker,LayerClosure closure) {
        this.invoker = invoker;
        this.closure = closure;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        Class<?> rawType = method.getDeclaringClass();
        Closure closure = rawType.getDeclaredAnnotation(Closure.class);
        if (closure == null || closure.value().isBlank()) {
            return null;
        }

        LayerClosureCIFHandler handler = invoker.getClosureCIFHandler(method);
        OpaquePointer result = null;
        LayerBuffer buffer = null;

        try {

            buffer = handler.allocParameters(this.closure,method,args);
            result = handler.call(method,buffer);

            SafeLong safeLong = method.getReturnType().getAnnotation(SafeLong.class);
            if (safeLong == null) {
                safeLong = method.getAnnotation(SafeLong.class);
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == void.class || returnType == Void.class) {

                return null;

            } else if(returnType == int.class || returnType == Integer.class) {

                return MemoryManager.readInt(result.getAddress(),0);

            } else if(returnType == long.class || returnType == Long.class) {

                if (safeLong != null) {
                    return MemoryManager.readLongLong(result.getAddress(),0);
                }
                return MemoryManager.readLong(result.getAddress(),0);

            } else if (returnType == float.class || returnType == Float.class) {

                return MemoryManager.readFloat(result.getAddress(),0);

            } else if (returnType == double.class || returnType == Double.class) {

                return MemoryManager.readDouble(result.getAddress(),0);

            } else if (returnType == short.class || returnType == Short.class) {

                return MemoryManager.readShort(result.getAddress(),0);

            } else if (returnType == boolean.class || returnType == Boolean.class) {

                if (result instanceof IntPointer) {

                    return (MemoryManager.readInt(result.getAddress(), 0) != 0);

                } else if (result instanceof BytePointer) {

                    return (MemoryManager.readByte(result.getAddress(), 0) != 0);

                } else if (result instanceof ShortPointer) {

                    return (MemoryManager.readShort(result.getAddress(), 0) != 0);

                }

                throw new RuntimeException("Can not cast to boolean return type");

            } else if (returnType == char.class || returnType == Character.class) {

                return MemoryManager.readByte(result.getAddress(), 0);

            } else if (returnType == byte.class || returnType == Byte.class) {

                return MemoryManager.readByte(result.getAddress(), 0);

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

            handler.freeParameters(buffer);

        }

    }

}
