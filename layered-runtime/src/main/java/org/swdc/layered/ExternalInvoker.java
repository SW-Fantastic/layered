package org.swdc.layered;


import org.swdc.layered.def.PlatformCallback;

import java.nio.ByteBuffer;

public class ExternalInvoker {

    public static native long loadLibrary(String absoluteLibraryPath);

    public static native void unloadLibrary(long library);

    public static native ByteBuffer getLibrarySymbols(long library);

    public static native long lookup(long library, String symbol);

    public static native String getLastError();

    public static native long createFFICIF(int[] argTypes, int returnType);

    public static native void destroyFFICIF(long cifAddress);

    public static native void call(long cifAddress, long resultAddr, long functionAddr, long addrOfArgs, int[] argTypes);

    public static native long createClosure(Object callback, long cifAddress);

    public static native long getClosureFunctionAddr(long closureAddress);

    public static native void freeClosure(long closureAddress);
}
