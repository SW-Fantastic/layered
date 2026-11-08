package org.swdc.layered.calling;

import org.swdc.layered.*;
import org.swdc.layered.library.LayerExceptions;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.pointers.OpaquePointer;
import org.swdc.layered.pointers.PtrPointer;
import org.swdc.layered.pointers.UniquePointer;
import org.swdc.layered.types.Closure;
import org.swdc.layered.types.SafeLong;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LibFFI的Closure，C/C++对Java的回调会以这种形式进行。
 */
public class LayerClosure<T> extends OpaquePointer {

    private T closureTarget;

    private Method closureMethod;

    private LayerFunction callback;

    private LayerInvoker invoker;

    private long closureAddress;

    private ReentrantLock lock = new ReentrantLock();

    public LayerClosure(Allocator allocator, LayerInvoker invoker, Class<?> closureType, T closureTarget) {

        super(allocator, 0, 0, true);

        LayerMetadata metadata = invoker.getMetadata();
        Map<String, LayerFunction> symbols = metadata.getSymbols();

        Closure target = closureType.getAnnotation(Closure.class);
        if (target == null) {
            throw new IllegalArgumentException("Class " + closureType.getName() + " is not annotated with @Closure");
        }

        for (Method method : closureType.getMethods()) {

            if (!method.getName().equals(target.value())) {
                continue;
            }

            String mangledName = TypeUtils.generateCbMangledName(method);
            if (!metadata.getSymbols().containsKey(mangledName)) {
                throw new RuntimeException("Method " + method.getName() + " does not matches with any callback in module , computed mangled name is " + mangledName);
            }

            LayerClosureCIFHandler cifHandler = invoker.getClosureCIFHandler(method);
            this.invoker = invoker;
            this.callback = symbols.get(mangledName);
            this.closureTarget = closureTarget;
            this.closureMethod = method;
            this.closureAddress = ExternalInvoker.createClosure(
                    this, cifHandler.getCifAddress()
            );

            break;

        }

        if (closureAddress == 0 || callback == null || closureMethod == null) {
            close();
        }

    }

    public LayerClosure(Allocator allocator,LayerInvoker invoker, Class<?> closureType, long handleAddress) {

        super(allocator, handleAddress, 0, false);

        LayerMetadata metadata = invoker.getMetadata();
        Map<String, LayerFunction> symbols = metadata.getSymbols();

        Closure target = closureType.getAnnotation(Closure.class);
        for (Method method : closureType.getMethods()) {

            if (!method.getName().equals(target.value())) {
                continue;
            }
            String mangledName = TypeUtils.generateCbMangledName(method);
            if (!symbols.containsKey(mangledName)) {
                throw new RuntimeException("Method " + method.getName() + " does not matches with any callback in module , computed mangled name is " + mangledName);
            }
            LayerClosureCIFHandler cifHandler = invoker.getClosureCIFHandler(method);
            this.invoker = invoker;
            this.callback = cifHandler.getFunction();
            this.closureMethod = method;
            this.closureAddress = 0;
            Object caller = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{
                    this.closureMethod.getDeclaringClass()
            }, new LayerClosureInvokeHandler(this.invoker,this));

            this.closureTarget = (T)caller;
            break;

        }


    }

    @Override
    public long getAddress() {
        return  this.closureAddress;
    }

    @Override
    public boolean isNull() {
        return isOwner() && closureAddress == 0 || this.closureTarget == null;
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

        if (!isOwner()) {
            LayerExceptions.throwException(new IllegalStateException("closureTarget is null"));
            return;
        }

        try {

            Allocator allocator = invoker.getAllocator();
            List<MetaType> paramTypes = callback.getParams();

            Parameter[] parameters = closureMethod.getParameters();
            Object[] args = new Object[closureMethod.getParameterCount()];
            PtrPointer pointer = allocator.borrowPtr(argsAddr);

            List<OpaquePointer> autoPointerList = new ArrayList<>();

            boolean hasError = false;

            for (int index = 0; index < closureMethod.getParameterCount(); index++) {
                OpaquePointer paramPtr = pointer.getAddress(index);
                // meta type 0是Closure自己的地址，所以取MetaType需要跳过它。
                MetaType type = paramTypes.get(index + 1);
                Parameter param = parameters[index];
                try {
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

                        try {

                            PtrPointer ptrPointer = new PtrPointer(paramPtr.move());
                            args[index] = ptrPointer.getAddress(0, param.getType());
                            ptrPointer.close();
                            autoPointerList.add((OpaquePointer) args[index]);

                        } catch (Exception e) {
                            LayerExceptions.throwException(
                                new InvocationTargetException(e, "Can not cast to target return type " +  param.getType().getName() + ", because no UniquePointer constructor found.")
                            );
                        }

                    }
                } finally {
                    paramPtr.close();
                }
            }

            pointer.close();

            if (hasError) {
                for (OpaquePointer autoPointer : autoPointerList) {
                    autoPointer.close();
                }
                return;
            }

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

            for (OpaquePointer autoPointer : autoPointerList) {
                if (autoPointer == null || autoPointer.isNull()) {
                    continue;
                }
                autoPointer.close();
            }
            autoPointerList.clear();

        } catch (Throwable e) {
            LayerExceptions.throwException(e);
        }

    }

    public long getFunctionAddress() {
        if (isOwner()) {
            return ExternalInvoker.getClosureFunctionAddr(closureAddress);
        } else {
            return closureAddress;
        }
    }

    public T getCaller() {

        if (this.closureTarget != null) {
            return closureTarget;
        } else {
            throw new IllegalStateException("Caller is null");
        }

    }

    public String getMangledName() {
        return callback.getMangledName();
    }

    protected void doUnsafeClear() {
        try {
            lock.lock();
            closureAddress = 0;
            closureTarget = null;
            closureMethod = null;
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        try {

            lock.lock();
            if (isOwner() && closureAddress != 0) {
                ExternalInvoker.freeClosure(closureAddress);
                getAllocator().freeClosure(closureAddress);
            }
            super.close();

        } finally {
            lock.unlock();
        }
    }


}
