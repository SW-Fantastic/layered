package org.swdc.layered.calling;

import org.swdc.layered.ExternalInvoker;
import org.swdc.layered.LayerFunction;
import org.swdc.layered.MetaType;

import java.util.concurrent.locks.ReentrantLock;

public class LayerClosureCIFHandler {

    private volatile long cifAddress;

    private ReentrantLock lock = new ReentrantLock();

    private LayerFunction callback;

    public LayerClosureCIFHandler(LayerFunction callback) {
        this.callback = callback;
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

    public LayerFunction getCallback() {
        return callback;
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
