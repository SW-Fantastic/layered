package org.swdc.layered.calling;

import org.swdc.layered.ExternalInvoker;
import org.swdc.layered.LayerFunction;
import org.swdc.layered.MemoryManager;
import org.swdc.layered.MetaType;
import org.swdc.layered.library.LayerExceptions;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.pointers.OpaquePointer;
import org.swdc.layered.pointers.PtrPointer;
import org.swdc.layered.pointers.UniquePointer;
import org.swdc.layered.types.SafeLong;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LibFFI的Closure，C/C++对Java的回调会以这种形式进行。
 */
public class LayerClosure<T> {

    private T closureTarget;

    private Method closureMethod;

    private LayerFunction callback;

    private LayerClosureCIFHandler cifHandler;

    private LayerInvoker invoker;

    private long closureAddress;

    private ReentrantLock lock = new ReentrantLock();

    public LayerClosure(LayerInvoker invoker, Method closureMethod, T closureTarget, LayerClosureCIFHandler cifHandler) {

        this.invoker = invoker;
        this.cifHandler = cifHandler;
        this.callback = cifHandler.getCallback();
        this.closureTarget = closureTarget;
        this.closureMethod = closureMethod;

    }

    public long getAddress() {

        if (closureAddress != 0) {
            return closureAddress;
        }

        try {
            lock.lock();
            this.closureAddress = ExternalInvoker.createClosure(
                    this,cifHandler.getCifAddress()
            );
            if (closureAddress == 0) {
                throw new IllegalStateException("Failed to create closure");
            }
            return closureAddress;
        } finally {
            lock.unlock();
        }

    }

    /**
     * 处理来自C/C++的回调调用，将原生参数转换为Java参数并执行目标方法，再把返回值写回原生内存。
     *
     * <p>该方法依据回调签名（{@link LayerFunction}）中记录的参数类型，从原生参数缓冲区
     * {@code argsAddr} 逐个读取参数值，并按Java方法参数的声明类型进行转换；若遇到无法转换的
     * 类型，则抛出异常并终止本次调用。参数解析完成后，通过反射调用 {@code closureMethod}，
     * 最后根据返回类型元数据将结果写入 {@code returnAddr} 指向的原生内存。</p>
     *
     * @param returnAddr 原生返回值缓冲区的地址，用于存放回调的返回结果；返回类型为 void 时忽略
     * @param argsAddr   原生参数缓冲区的地址，其内容为按回调签名顺序排列的各个参数值
     */
    private void byCall(long returnAddr, long argsAddr) {

        Allocator allocator = invoker.getAllocator();
        List<MetaType> paramTypes = callback.getParams();

        Parameter[] parameters = closureMethod.getParameters();
        Object[] args = new Object[callback.getParams().size()];
        PtrPointer pointer = allocator.borrowPtr(argsAddr);

        boolean hasError = false;

        for (int index = 0; index < closureMethod.getParameterCount(); index++) {
            OpaquePointer paramPtr = pointer.getAddress(index);
            MetaType type = paramTypes.get(index);
            Parameter param = parameters[index];
            if (type == MetaType.INT) {

                int value = MemoryManager.readInt(paramPtr.getAddress(), 0);
                if (param.getType() == int.class || param.getType() == Integer.class) {
                    args[index] = value;
                } else if (param.getType() == long.class || param.getType() == Long.class) {
                    args[index] = value;
                } else if(param.getType() == boolean.class || param.getType() == Boolean.class) {
                    args[index] = value != 0;
                } else {
                    LayerExceptions.throwException(
                            new IllegalArgumentException("Illegal type for parameter " + param.getName())
                    );
                }

            } else if (type == MetaType.LONG) {

                long value = 0L;
                SafeLong safeLong = param.getAnnotation(SafeLong.class);
                if (safeLong != null) {
                    // 64位整数,long long类型
                    value = MemoryManager.readLongLong(paramPtr.getAddress(), 0);
                } else {
                    value = MemoryManager.readLong(paramPtr.getAddress(), 0);
                }

                if (param.getType() == long.class || param.getType() == Long.class) {
                    args[index] = value;
                } else if (param.getType() == int.class || param.getType() == Integer.class) {
                    args[index] = (int) value;
                } else if (param.getType() == short.class || param.getType() == Short.class) {
                    args[index] = (short) value;
                } else if(param.getType() == boolean.class || param.getType() == Boolean.class) {
                    args[index] = value != 0;
                } else {
                    LayerExceptions.throwException(
                            new IllegalArgumentException("Illegal type for parameter " + param.getName())
                    );
                    hasError = true;
                }

            } else if (type == MetaType.FLOAT) {

                float value = MemoryManager.readFloat(paramPtr.getAddress(), 0);
                if (param.getType() == float.class || param.getType() == Float.class) {
                    args[index] = value;
                } else if (param.getType() == double.class || param.getType() == Double.class) {
                    args[index] = (double) value;
                } else {
                    LayerExceptions.throwException(
                            new IllegalArgumentException("Illegal type for parameter " + param.getName())
                    );
                    hasError = true;
                }

            } else if (type == MetaType.DOUBLE) {

                double value = MemoryManager.readDouble(paramPtr.getAddress(), 0);
                if (param.getType() == double.class || param.getType() == Double.class) {
                    args[index] = value;
                } else {
                    LayerExceptions.throwException(
                            new IllegalArgumentException("Illegal type for parameter " + param.getName())
                    );
                    hasError = true;
                }

            } else if (type == MetaType.SHORT) {

                short value = MemoryManager.readShort(paramPtr.getAddress(), 0);
                if (param.getType() == short.class || param.getType() == Short.class) {
                    args[index] = value;
                } else if (param.getType() == int.class || param.getType() == Integer.class) {
                    args[index] = (int) value;
                } else if (param.getType() == long.class || param.getType() == Long.class) {
                    args[index] = (long) value;
                } else {
                    LayerExceptions.throwException(
                            new IllegalArgumentException("Illegal type for parameter " + param.getName())
                    );
                    hasError = true;
                }

            } else if (type == MetaType.CHAR) {

                byte value = MemoryManager.readByte(paramPtr.getAddress(), 0);
                if (param.getType() == byte.class || param.getType() == Byte.class) {
                    args[index] = value;
                } else if (param.getType() == char.class || param.getType() == Character.class) {
                    args[index] = (char) value;
                } else if (param.getType() == int.class || param.getType() == Integer.class) {
                    args[index] = (int) value;
                } else if(param.getType() == boolean.class || param.getType() == Boolean.class) {
                    args[index] = value != 0;
                } else if (param.getType() == long.class || param.getType() == Long.class) {
                    args[index] = (long) value;
                } else if (param.getType() == short.class || param.getType() == Short.class) {
                    args[index] = (short) value;
                } else {
                    LayerExceptions.throwException(
                            new IllegalArgumentException("Illegal type for parameter " + param.getName())
                    );
                    hasError = true;
                }

            } else if (type == MetaType.LONG_LONG) {

                long value = MemoryManager.readLongLong(paramPtr.getAddress(), 0);
                if (param.getType() == long.class || param.getType() == Long.class) {
                    args[index] = value;
                } else {
                    LayerExceptions.throwException(
                            new IllegalArgumentException("Illegal type for parameter " + param.getName())
                    );
                    hasError = true;
                }

            } else if (type == MetaType.SIZE_T) {

                long value = MemoryManager.readSizeT(paramPtr.getAddress(), 0);
                if (param.getType() == long.class || param.getType() == Long.class) {
                    args[index] = value;
                } else {
                    LayerExceptions.throwException(
                            new IllegalArgumentException("Illegal type for parameter " + param.getName())
                    );
                    hasError = true;
                }

            } else if (type == MetaType.INTPTR_T) {

                long addr = MemoryManager.readAddress(paramPtr.getAddress(), 0);
                if (addr == 0) {
                    args[index] = null;
                } else {
                    try {
                        OpaquePointer result = allocator.borrow(addr);
                        PtrPointer ptrPointer = new PtrPointer(result.offset(0).move());
                        Object resultPtr = ptrPointer.getAddress(0, param.getType());
                        ptrPointer.close();
                        args[index] = resultPtr;

                    } catch (Exception e) {
                        LayerExceptions.throwException(
                                new IllegalArgumentException("Illegal type for parameter " + param.getName())
                        );
                        hasError = true;
                    }
                }
            }
        }

        pointer.close();

        if (hasError) {
            return;
        }

        try {

            Class<?> closureReturnType = closureMethod.getReturnType();
            MetaType returnType = callback.getReturnType();
            Object result = closureMethod.invoke(closureTarget, args);
            if (returnType == MetaType.VOID) {
                return;
            } else if (returnType == MetaType.INT) {
                if (closureReturnType == Boolean.class || closureReturnType == boolean.class) {
                    result = (Boolean) result ? 1 : 0;
                }
                MemoryManager.writeInt(returnAddr, 0, (int) result);
            } else if (returnType == MetaType.SHORT) {
                if (closureReturnType == Boolean.class || closureReturnType == boolean.class) {
                    result = (Boolean) result ? 1 : 0;
                }
                MemoryManager.writeShort(returnAddr, 0, (short) result);
            } else if (returnType == MetaType.CHAR) {
                if (closureReturnType == Boolean.class || closureReturnType == boolean.class) {
                    result = (Boolean) result ? 1 : 0;
                }
                MemoryManager.writeByte(returnAddr, 0, (byte) result);
            } else if (returnType == MetaType.LONG_LONG) {
                if (closureReturnType == Boolean.class || closureReturnType == boolean.class) {
                    result = (Boolean) result ? 1 : 0;
                }
                MemoryManager.writeLongLong(returnAddr, 0, (long) result);
            } else if (returnType == MetaType.SIZE_T) {
                if (closureReturnType == Boolean.class || closureReturnType == boolean.class) {
                    result = (Boolean) result ? 1 : 0;
                }
                long value = (long) result;
                if (value < 0) {
                    LayerExceptions.throwException(
                            new IllegalArgumentException("Size_t should not be negative")
                    );
                    MemoryManager.writeSizeT(returnAddr, 0, 0);
                } else {
                    MemoryManager.writeSizeT(returnAddr, 0, value);
                }
            } else if(returnType == MetaType.FLOAT) {
                MemoryManager.writeFloat(returnAddr, 0, (float) result);
            } else if (returnType == MetaType.DOUBLE) {
                MemoryManager.writeDouble(returnAddr, 0, (double) result);
            } else if (returnType == MetaType.LONG) {
                long val = (long) result;
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.contains("win")) {
                    LayerExceptions.throwException(
                            new IllegalArgumentException("long value is too large for return type on Windows")
                    );
                    val = (int)val;
                }
                MemoryManager.writeLong(returnAddr, 0, val);
            } else if (returnType == MetaType.INTPTR_T) {

                long addr = 0;
                if (result instanceof OpaquePointer) {
                    OpaquePointer resultPtr = (OpaquePointer)result;
                    addr = resultPtr.getAddress();
                } else if (result instanceof UniquePointer) {
                    UniquePointer resultPtr = (UniquePointer)result;
                    addr = resultPtr.unmanaged();
                } else {
                    LayerExceptions.throwException(
                            new IllegalArgumentException("Illegal type for return value, not a valid pointer type")
                    );
                }
                MemoryManager.writeAddress(returnAddr, 0, addr);
            } else {
                LayerExceptions.throwException(
                        new IllegalArgumentException("Illegal return type for callback")
                );
            }

        } catch (Throwable e) {
            LayerExceptions.throwException(e);
        }

    }

    public void close() {
        if (closureAddress == 0) {
            return;
        }
        try {
            lock.lock();
            ExternalInvoker.freeClosure(closureAddress);
            closureAddress = 0;
            invoker.closureUnref(closureMethod, this);
        } finally {
            lock.unlock();
        }
    }


}
