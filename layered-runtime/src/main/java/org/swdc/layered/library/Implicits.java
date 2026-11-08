package org.swdc.layered.library;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 隐式上下文，全局性的注册和获取本地库实例，
 * API或者SDK的设计者应该根据自己的需要使用它。
 */
public class Implicits {

    private static final Map<Class, Object> loadedLibraries = new ConcurrentHashMap<>();

    private static ReadWriteLock lock = new ReentrantReadWriteLock();

    public static <T> T getInstance(Class<?> clazz) {
        try {
            lock.readLock().lock();
            if (loadedLibraries.containsKey(clazz)) {
                return (T)loadedLibraries.get(clazz);
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }

    }

    public static void registerLibrary(Class<?> clazz, Object library) {
        try {
            lock.writeLock().lock();
            if (loadedLibraries.containsKey(clazz)) {
                throw new IllegalStateException("Library already registered " + clazz.getName());
            }
            loadedLibraries.put(clazz, library);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void unregisterLibrary(Class<?> clazz) {
        try {
            lock.writeLock().lock();
            if (!loadedLibraries.containsKey(clazz)) {
                return;
            }
            loadedLibraries.remove(clazz);
        } finally {
            lock.writeLock().unlock();
        }

    }


}
