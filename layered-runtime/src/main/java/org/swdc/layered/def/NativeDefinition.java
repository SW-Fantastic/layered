package org.swdc.layered.def;

public enum NativeDefinition {

    CLASS,
    STRUCT,
    ENUM,
    FUNCTION,
    CALLBACK,
    ARRAY,
    POINTER,

    // Primitive types

    INT,
    CHAR,
    FLOAT,
    DOUBLE,
    LONG_DOUBLE,
    SHORT,
    LONG,
    LONGLONG,
    UNSIGNED_INT,
    UNSIGNED_CHAR,
    UNSIGNED_SHORT,
    UNSIGNED_LONG,
    UNSIGNED_LONGLONG,
    BOOL,
    VOID,

    // Platform types
    // 这些类型是基本类型的拓展类型，通过别名声明，它们长度是可变的，在wrapper统一处理为long。
    SIZE_T(true),
    SSIZE_T(true),
    TIME_T(true),
    PTR_DIFF_T(true);

    private boolean platformDep;

    NativeDefinition() {
        platformDep = false;
    }

    NativeDefinition(boolean platformDep) {
        this.platformDep = platformDep;
    }

    public boolean isPlatformDepType() {
        return platformDep;
    }
}
