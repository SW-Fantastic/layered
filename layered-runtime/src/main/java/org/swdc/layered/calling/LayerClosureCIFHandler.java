package org.swdc.layered.calling;

import org.swdc.layered.ExternalInvoker;
import org.swdc.layered.LayerFunction;
import org.swdc.layered.MemoryManager;
import org.swdc.layered.MetaType;
import org.swdc.layered.pointers.OpaquePointer;
import org.swdc.layered.pointers.UniquePointer;
import org.swdc.layered.types.Unsigned;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.locks.ReentrantLock;

public class LayerClosureCIFHandler extends LayerCIFHandler {

    private volatile long cifAddress;

    private ReentrantLock lock = new ReentrantLock();

    public LayerClosureCIFHandler(LayerInvoker invoker, LayerFunction callback) {
        super(invoker,callback);
    }

    /**
     * 获取回调函数的 FFI CIF（调用接口描述）地址，用于原生层按正确签名调用该回调。
     *
     * <p>采用懒加载方式：首次调用时收集回调各参数及返回值的类型标志，
     * 通过 {@link ExternalInvoker#createFFICIF} 创建 CIF 并缓存；后续调用直接返回缓存地址。
     * 创建过程通过锁保证并发安全，创建失败时抛出异常。</p>
     *
     * @return CIF 的原生地址
     * @throws IllegalStateException 当 CIF 创建失败时抛出
     */
    public long getCifAddress() {

        if (cifAddress != 0) {
            return cifAddress;
        }

        try {

            lock.lock();

            LayerFunction callback = getFunction();
            int[] argTypes = new int[callback.getParams().size()];
            for (int i = 0; i < callback.getParams().size(); i++) {
                MetaType type = callback.getParams().get(i);
                argTypes[i] = TypeUtils.getInvokerType(type).getFlag();
            }

            MetaType returnType = callback.getReturnType();
            int returnTypeFlag = TypeUtils.getInvokerType(returnType).getFlag();
            cifAddress = ExternalInvoker.createFFICIF(argTypes, returnTypeFlag);
            if (cifAddress == 0) {
                throw new IllegalStateException("Failed to create CIF");
            }
            return cifAddress;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public LayerBuffer allocParameters(Method method, Object... args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public LayerBuffer allocParameters(LayerClosure closure, Method method, Object... args) {

        Parameter[] parameters = method.getParameters();
        LayerBuffer buffer = getBufferPool().borrow();
        buffer.reset();

        long moduleAddress = getInvoker().getModuleAddress();
        if (moduleAddress == 0) {
            throw new RuntimeException("Module address is null");
        }

        MemoryManager.writeAddress(
                buffer.getParameterBuf(0).getAddress(),
                0,
                closure.getFunctionAddress()
        );

        for (int i = 1; i < getFunction().getParams().size(); i++) {
            int realIdx = i - 1;
            MetaType metaType = getFunction().getParams().get(i);
            if (metaType == MetaType.INT) {

                Integer val = null;
                if (args[i].getClass() == Boolean.class) {
                    val = ((Boolean)args[realIdx]) ? 1 : 0;
                } else if (args[i].getClass() == Integer.class) {
                    val = (Integer)args[realIdx];
                } else {
                    val = 0;
                }
                MemoryManager.writeInt(buffer.getParameterBuf(i).getAddress(), 0, val);

            } else if(metaType == MetaType.FLOAT){

                Float fval = (Float)args[realIdx];
                if (fval == null) {
                    fval = 0.0f;
                }
                MemoryManager.writeFloat(buffer.getParameterBuf(i).getAddress(), 0, fval);

            } else if (metaType == MetaType.LONG){

                Long val = (Long)args[realIdx];
                if (val == null) {
                    val = 0L;
                }

                Parameter param = parameters[realIdx];
                if (param.getAnnotation(Unsigned.class) != null && val < 0) {
                    throw new IllegalArgumentException("Unsigned long value is negative: " + val);
                }

                String osType = System.getProperty("os.name").toLowerCase();
                if (osType.contains("windows") && val >= Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Long value is too large for Windows platform: " + val);
                }
                MemoryManager.writeLong(buffer.getParameterBuf(i).getAddress(), 0, val);

            } else if (metaType == MetaType.LONG_LONG) {

                Long val = (Long)args[realIdx];
                if (val == null) {
                    val = 0L;
                }

                Parameter param = parameters[realIdx];
                if (param.getAnnotation(Unsigned.class) != null && val < 0) {
                    throw new IllegalArgumentException("Unsigned long value is negative: " + val);
                }

                MemoryManager.writeLong(buffer.getParameterBuf(i).getAddress(), 0, val);

            } else if (metaType == MetaType.DOUBLE){

                Double val = (Double)args[realIdx];
                if (val == null) {
                    val = 0D;
                }
                MemoryManager.writeDouble(buffer.getParameterBuf(i).getAddress(), 0, val);

            } else if (metaType == MetaType.INTPTR_T) {

                Long addr = 0L;
                if (args[realIdx] instanceof UniquePointer) {
                    addr = ((UniquePointer<?>)args[realIdx]).unmanaged();
                } else if (args[realIdx] instanceof LayerClosure) {
                    LayerClosure layerClosure = (LayerClosure)args[realIdx];
                    addr = layerClosure.getFunctionAddress();
                } else if (args[realIdx] instanceof OpaquePointer) {
                    addr = ((OpaquePointer)args[realIdx]).getAddress();
                }
                MemoryManager.writeAddress(buffer.getParameterBuf(i).getAddress(), 0, addr);

            } else if (metaType == MetaType.SIZE_T) {

                Long val = 0L;
                if (args[realIdx] instanceof Number) {
                    val = (Long) args[realIdx];
                }

                Parameter param = parameters[realIdx];
                if (param.getAnnotation(Unsigned.class) != null && val < 0) {
                    throw new IllegalArgumentException("Unsigned long value is negative: " + val);
                }

                MemoryManager.writeSizeT(buffer.getParameterBuf(i).getAddress(), 0, val);

            } else if (metaType == MetaType.SHORT) {

                Short val = null;
                if (args[realIdx] instanceof Short) {
                    val = (Short) args[realIdx];
                } else if (args[realIdx] instanceof Boolean) {
                    val = ((Boolean) args[realIdx]) ? (short)1 : (short)0;
                }

                MemoryManager.writeShort(buffer.getParameterBuf(i).getAddress(), 0, val);

            } else if (metaType == MetaType.CHAR) {

                byte val = 0x00;
                if (args[realIdx] instanceof Character) {
                    Character ch = (Character)args[realIdx];
                    val = (byte) ch.charValue();
                } else if (args[realIdx] instanceof Boolean) {
                    Boolean b = (Boolean)args[realIdx];
                    val = (byte) (b ? 1 : 0);
                }
                MemoryManager.writeByte(buffer.getParameterBuf(i).getAddress(), 0, val);

            } else {
                throw new RuntimeException("Unknown meta type : " +  metaType);
            }
        }
        return buffer;
    }

    public void close() {
        if (cifAddress == 0) {
            return;
        }
        try {
            lock.lock();
            ExternalInvoker.destroyFFICIF(cifAddress);
            cifAddress = 0;
        } finally {
            lock.unlock();
        }
    }

}
