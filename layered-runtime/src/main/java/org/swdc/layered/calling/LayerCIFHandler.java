package org.swdc.layered.calling;

import org.swdc.layered.ExternalInvoker;
import org.swdc.layered.LayerFunction;
import org.swdc.layered.MemoryManager;
import org.swdc.layered.MetaType;
import org.swdc.layered.pointers.*;

import java.util.concurrent.locks.ReentrantLock;

public class LayerCIFHandler {

    private Long cifAddress;

    private LayerFunction function;

    private LayerInvoker invoker;

    private ReentrantLock lock = new  ReentrantLock();

    private int[] ffiArgTypes;


    public LayerCIFHandler(LayerInvoker invoker, LayerFunction function) {
        this.function = function;
        this.invoker = invoker;
    }

    /**
     * 获取本地函数调用的 CIF（Call Interface）地址
     *
     * <p>采用懒加载方式创建 CIF：首次调用时依据函数的参数类型和返回值类型，
     * 通过 {@link TypeUtils#getInvokerType(MetaType)} 解析出各参数对应的调用类型标识，
     * 再交由 {@link ExternalInvoker#createFFICIF(int[], int)} 生成 CIF 并缓存；
     * 后续调用直接返回已缓存的地址。</p>
     *
     * <p>整个过程由 {@link ReentrantLock} 保护，以保证多线程环境下的线程安全。</p>
     *
     * @return CIF 的本地内存地址
     * @throws RuntimeException 当 CIF 创建失败（返回地址为 0）时抛出
     */
    public long getCifAddress() {

        try {
            lock.lock();
            if (cifAddress != null) {
                return cifAddress;
            }

            int[] argTypes = new int[function.getParams().size()];
            for (int i = 0; i < function.getParams().size(); i++) {
                MetaType type = function.getParams().get(i);
                argTypes[i] = TypeUtils.getInvokerType(type).getFlag();
            }

            MetaType returnType = function.getReturnType();
            int returnTypeFlag = TypeUtils.getInvokerType(returnType).getFlag();

            long cifAddr = ExternalInvoker.createFFICIF(argTypes,returnTypeFlag);
            if (cifAddr == 0) {
                throw new RuntimeException("Failed to create CIF");
            }
            this.cifAddress = cifAddr;
            this.ffiArgTypes = argTypes;

            return cifAddr;
            
        } finally {
            lock.unlock();
        }
        
    }

    /**
     * 将Java参数转换为本地函数调用所需的指针数组
     *
     * <p>依据函数的元数据类型（{@link MetaType}）为每个参数分配对应的内存指针，
     * 并将参数值写入所分配的内存中。对于 Boolean 参数会按整数（1 或 0）处理；
     * 对于 INTPTR_T / SIZE_T 会从 {@link UniquePointer} 或 {@link OpaquePointer}
     * 中提取地址。当参数类型的字节数小于平台指针时，分配容量会对齐到指针大小，
     * 以保证跨平台调用的兼容性。</p>
     *
     * @param args 待转换的参数列表，顺序需与函数参数定义一致
     * @return 转换后的指针数组，每个元素按顺序对应一个函数参数
     * @throws RuntimeException 当 moduleAddress 为 0，或遇到未知的元数据类型时抛出
     */
    public void transformParameters(LayerBuffer buffer, Object... args) {
        try {

            lock.lock();

            long moduleAddress = invoker.getModuleAddress();
            if (moduleAddress == 0) {
                throw new RuntimeException("Module address is null");
            }

            for (int i = 0; i < function.getParams().size(); i++) {
                MetaType metaType = function.getParams().get(i);
                if (metaType == MetaType.INT) {

                    Integer val = null;
                    if (args[i].getClass() == Boolean.class) {
                        val = ((Boolean)args[i]) ? 1 : 0;
                    } else if (args[i].getClass() == Integer.class) {
                        val = (Integer)args[i];
                    } else {
                        val = 0;
                    }
                    MemoryManager.writeInt(buffer.getParameterBuf(i).getAddress(), 0, val);

                } else if(metaType == MetaType.FLOAT){

                    Float fval = (Float)args[i];
                    if (fval == null) {
                        fval = 0.0f;
                    }
                    MemoryManager.writeFloat(buffer.getParameterBuf(i).getAddress(), 0, fval);

                } else if (metaType == MetaType.LONG){

                    Long val = (Long)args[i];
                    if (val == null) {
                        val = 0L;
                    }
                    MemoryManager.writeLong(buffer.getParameterBuf(i).getAddress(), 0, val);

                } else if (metaType == MetaType.DOUBLE){

                    Double val = (Double)args[i];
                    if (val == null) {
                        val = 0D;
                    }
                    MemoryManager.writeDouble(buffer.getParameterBuf(i).getAddress(), 0, val);

                } else if (metaType == MetaType.INTPTR_T || metaType == MetaType.SIZE_T) {

                    Long addr = 0L;
                    if (args[i] instanceof UniquePointer) {
                        addr = ((UniquePointer<?>)args[i]).getAddress();
                    } else if (args[i] instanceof OpaquePointer) {
                        addr = ((OpaquePointer)args[i]).getAddress();
                    }
                    MemoryManager.writeAddress(buffer.getParameterBuf(i).getAddress(), 0, addr);

                } else if (metaType == MetaType.SHORT) {

                    Short val = null;
                    if (args[i] instanceof Short) {
                        val = (Short) args[i];
                    } else if (args[i] instanceof Boolean) {
                        val = ((Boolean) args[i]) ? (short)1 : (short)0;
                    }

                    MemoryManager.writeShort(buffer.getParameterBuf(i).getAddress(), 0, val);

                } else if (metaType == MetaType.CHAR) {

                    byte val = 0x00;
                    if (args[i] instanceof Character) {
                        Character ch = (Character)args[i];
                        val = (byte) ch.charValue();
                    } else if (args[i] instanceof Boolean) {
                        Boolean b = (Boolean)args[i];
                        val = (byte) (b ? 1 : 0);
                    }
                    MemoryManager.writeByte(buffer.getParameterBuf(i).getAddress(), 0, val);

                } else {
                    throw new RuntimeException("Unknown meta type : " +  metaType);
                }
            }

        } finally {
            lock.unlock();
        }
    }

    /**
     * 调用本地函数并返回结果指针
     *
     * <p>执行完整的FFI调用流程：首先查找本地函数地址，然后根据返回值类型分配结果内存，
     * 将参数指针数组传递给底层 {@link ExternalInvoker#call} 执行调用，
     * 调用完成后自动释放参数指针的内存。</p>
     *
     * <p>返回值类型支持：INT、FLOAT、LONG、DOUBLE、INTPTR_T、SIZE_T、SHORT、CHAR。</p>
     *
     * @return 指向函数返回值的指针，调用方负责在使用完毕后释放内存
     * @throws RuntimeException 当模块地址、CIF地址或函数地址任一为0时抛出
     */
    public OpaquePointer call(LayerBuffer buffer) {

        try {

            lock.lock();

            long functionAddress = invoker.lookupMethodAddress(function);
            long moduleAddress = invoker.getModuleAddress();
            long cifAddress = getCifAddress();

            if (moduleAddress == 0 || cifAddress == 0 ||  functionAddress == 0) {
                throw new RuntimeException("Module address or cifAddress is null");
            }

            OpaquePointer result = buffer.getResultBuf();
            OpaquePointer params = buffer.getParamPtr();

            ExternalInvoker.call(
                    cifAddress,
                    result.getAddress(),
                    functionAddress,
                    params.getAddress(),
                    ffiArgTypes
            );

            params.close();
            return result;

        } finally {
            lock.unlock();
        }

    }

    public void close() {
        try {
            lock.lock();
            if (cifAddress != null) {
                ExternalInvoker.destroyFFICIF(cifAddress);
            }
            invoker = null;
            cifAddress = null;
            function = null;
            ffiArgTypes = null;
        } finally {
            lock.unlock();
        }
    }

}
