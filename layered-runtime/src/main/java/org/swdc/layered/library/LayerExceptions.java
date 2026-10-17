package org.swdc.layered.library;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *  Callback的异常处理，当一个异常来自Closure的时候（可能是独立的函数Callback或者是重写的虚方法等，
 *  总之这个Java异常出现在Native调用Java的过程中），异常将会被本类接管。
 *  异常将会被收集到一个单独的线程中，并被逐个处理，注册异常处理器到本类，可以单独处置某一种异常类型，
 *  否则，捕获的异常堆栈将会直接输出到系统异常流。
 */
public class LayerExceptions {

    private static LayerExceptions INSTANCE = new LayerExceptions();

    /**
     * 异常处理线程，用于收集并逐个处理异常。
     */
    private Thread shadowThread;

    /**
     * 异常队列，用于收集所有捕获的异常。
     */
    private List<Throwable> exceptions = new ArrayList<>();

    /**
     * 异常处理器，用于处理特定类型的异常。
     */
    private Map<Class<? extends Exception>, LayerExceptionHandler>  handlers = new HashMap<>();

    /**
     * 锁，用于同步访问异常队列和处理器。
     */
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private LayerExceptions() {

        this.shadowThread = new Thread(this::run);
        this.shadowThread.setName("Layer exception handler");
        this.shadowThread.setDaemon(true);
        this.shadowThread.start();

    }

    /**
     * 异常处理线程的执行逻辑。
     * 循环从异常队列中取出所有已捕获的异常并逐个处理：
     * 按注册顺序查找能够匹配异常类型（或其父类）的处理器并调用，
     * 若无匹配的处理器，则将异常堆栈输出到系统异常流。
     * 处理完成后清空队列，并挂起当前线程等待下一次唤醒。
     */
    private void run() {

        while (true) {
            try {
                lock.readLock().lock();

                for (Throwable t : this.exceptions) {
                    boolean handled = false;
                    for (Class<? extends Exception> clazz : this.handlers.keySet()) {
                        if (clazz.isAssignableFrom(t.getClass())) {
                            LayerExceptionHandler handler = handlers.get(clazz);
                            handler.handle(t);
                            handled = true;
                            break;
                        }
                    }
                    if (!handled) {
                        t.printStackTrace(System.err);
                    }
                }
                exceptions.clear();

            } finally {
                lock.readLock().unlock();
            }

            LockSupport.park(this);
        }

    }

    /**
     * 添加异常处理器。
     * 当捕获到指定类型或其子类的异常时，将调用对应的处理器进行处理。
     *
     * @param exception 异常类型，用于匹配待处理的异常
     * @param handler   异常处理器，用于处理匹配到的异常
     * @throws IllegalArgumentException 当 handler 为 null 时抛出
     */
    void addHandler(Class<? extends Exception> exception, LayerExceptionHandler handler) {
        try {
            if (handler == null) {
                throw new IllegalArgumentException("Handler cannot be null");
            }
            lock.writeLock().lock();
            this.handlers.put(exception, handler);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 向异常处理队列添加一个异常，并唤醒异常处理线程。
     * 该方法主要用于 Native 调用 Java 过程中捕获的异常，
     * 异常将被异步处理：先入队列，然后唤醒后台线程逐个处理。
     *
     * @param t 待处理的异常对象
     */
    public static void throwException(Throwable t) {
        INSTANCE.exceptions.add(t);
        LockSupport.unpark(INSTANCE.shadowThread);
    }

    public static void addExceptionHandler(Class<? extends Exception> exception, LayerExceptionHandler handler) {
        INSTANCE.addHandler(exception, handler);
    }


}
