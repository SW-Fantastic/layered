package org.swdc.layered.calling;


import org.swdc.layered.MemoryManager;
import org.swdc.layered.pointers.*;
import org.swdc.layered.types.SafeLong;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 本地函数分发器，分发Native方法调用到native函数，
 * 实现具体的本地模块调用和相关功能。
 */
public class LayerInvokeHandler implements InvocationHandler {

    private LayerInvoker invoker;

    public LayerInvokeHandler(LayerInvoker invoker) {
        this.invoker = invoker;
    }

    /**
     * 代理调用入口，负责将 Java 方法调用转发到底层动态库并转换返回值。
     *
     * 若调用的是 Object 类声明的方法，则直接在本处理器实例上反射执行；否则通过
     * CIF 处理器将入参写入缓冲区后执行原生调用，再依据方法声明的返回类型，将结果
     * 指针解引用为对应的 Java 基本类型或包装类型。对于 OpaquePointer 及其子类型，
     * 则借助 PtrPointer 构造包装对象。调用出现异常时释放结果指针，并在 finally 中
     * 统一释放入参缓冲区。
     *
     * @param proxy 代理对象本身
     * @param method 被调用的方法，用于获取对应的 CIF 处理器及返回类型
     * @param args 方法调用入参
     * @return 转换后的返回值，void 方法返回 null
     * @throws Throwable 当原生调用失败或返回值类型无法转换时抛出
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        LayerCIFHandler handler = invoker.getCIFHandler(method);
        OpaquePointer result = null;
        LayerBuffer buffer = null;

        try {

            buffer = handler.allocParameters(method,args);
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

            buffer.reset();
            handler.freeParameters(buffer);

        }

    }

    @Override
    public String toString() {
        return "LayerInvokeHandler { at native address " + Long.toHexString(this.invoker.getModuleAddress()) + " }";
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

}
