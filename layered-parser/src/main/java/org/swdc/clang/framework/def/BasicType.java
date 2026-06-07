package org.swdc.clang.framework.def;

import org.swdc.layered.def.NativeDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基础类型，例如：int, float等。
 */
public class BasicType extends AbstractNativeType {

    private static final String SUBFIX_CONST = "?const";
    private static final String SUBFIX_VOLATILE = "?volatile";
    private static final String SUBFIX_CV = "?const&volatile";

    private static final String SUBFIX_M_CONST = "[C]";
    private static final String SUBFIX_M_VOLATILE = "[V]";
    private static final String SUBFIX_M_CV = "[CV]";
    private static final String SUBFIX_M_NORMAL = "[]";

    public static final BasicType CHAR = new BasicType("char", "C",  NativeDefinition.CHAR, "char");
    public static final BasicType INT = new BasicType("int", "I", NativeDefinition.INT, "int");
    public static final BasicType FLOAT = new BasicType("float", "F", NativeDefinition.FLOAT, "float");
    public static final BasicType DOUBLE = new BasicType("double", "D", NativeDefinition.DOUBLE, "double");
    public static final BasicType SHORT = new BasicType("short","S",NativeDefinition.SHORT, "short");
    public static final BasicType LONG = new BasicType("long","L", NativeDefinition.LONG, "long");
    public static final BasicType LONGLONG = new BasicType("long long","Ll", NativeDefinition.LONGLONG, "long");
    public static final BasicType UNSIGNED_INT = new BasicType("unsigned int","Ui", NativeDefinition.UNSIGNED_INT, "int");
    public static final BasicType UNSIGNED_CHAR = new BasicType("unsigned char","Uc", NativeDefinition.UNSIGNED_CHAR, "char");
    public static final BasicType UNSIGNED_SHORT = new BasicType("unsigned short","Us", NativeDefinition.UNSIGNED_SHORT, "short");
    public static final BasicType UNSIGNED_LONG = new BasicType("unsigned long","Ul", NativeDefinition.UNSIGNED_LONG, "long");
    public static final BasicType UNSIGNED_LONGLONG = new BasicType("unsigned long long", "Ull", NativeDefinition.UNSIGNED_LONGLONG, "long");
    public static final BasicType BOOL = new BasicType("bool","B", NativeDefinition.BOOL, "bool");
    public static final BasicType VOID = new BasicType("void","V", NativeDefinition.VOID, "void");
    public static final BasicType SIZE_T = new BasicType("size_t","Sz", NativeDefinition.SIZE_T, "size_t");
    public static final BasicType SSIZE_T = new BasicType("ssize_t","Ssz", NativeDefinition.SSIZE_T, "ssize_t");
    public static final BasicType TIME_T = new BasicType("time_t","Ts", NativeDefinition.TIME_T, "time_t");

    private static Map<String, BasicType> types = new ConcurrentHashMap<>();

    private String mangledFlag;

    private String base;

    private BasicType(String name, String mangledFlag, NativeDefinition type, String base) {
        super(name, type);
        this.mangledFlag = mangledFlag;
        this.base = base;
    }

    @Override
    public NameCaster castAsBaseType() {
        return (name) -> {
            if (name == null || name.isEmpty()) {
                if (getType().isPlatformDepType()) {
                    return "long";
                }
                return base;
            }
            if (getType().isPlatformDepType()) {
                return "long " + name;
            }
            return base + " " + name;
        };
    }


    BasicType constType() {
        BasicType constType = new BasicType(getName(), mangledFlag, getType(), base);
        constType.setConstType(true);
        return constType;
    }

    BasicType volatileType() {
        BasicType volatileType = new BasicType(getName(), mangledFlag, getType(), base);
        volatileType.setVolatileType(true);
        return volatileType;
    }

    BasicType constVolatileType() {
        BasicType constVolatileType = new BasicType(getName(), mangledFlag, getType(), base);
        constVolatileType.setConstType(true);
        constVolatileType.setVolatileType(true);
        return constVolatileType;
    }

    public String getMangledFlag() {
        if (isConstType() && isVolatileType()) {
            return mangledFlag + SUBFIX_M_CV;
        } else if (isConstType()) {
            return mangledFlag + SUBFIX_M_CONST;
        } else if (isVolatileType()) {
            return mangledFlag + SUBFIX_M_VOLATILE;
        } else {
            return mangledFlag;
        }
    }

    public String getSimpleMangledFlag() {
        return mangledFlag;
    }

    public static BasicType getByName(String name) {
        if (types.isEmpty()) {
            synchronized (BasicType.class) {

                if (!types.isEmpty()) {
                    return getByName(name);
                }

                types.put(CHAR.getName(), CHAR);
                types.put(CHAR.getName() + SUBFIX_CONST, CHAR.constType());
                types.put(CHAR.getName() + SUBFIX_VOLATILE, CHAR.volatileType());
                types.put(CHAR.getName() + SUBFIX_CV, CHAR.constVolatileType());

                types.put(INT.getName(), INT);
                types.put(INT.getName() + SUBFIX_CONST, INT.constType());
                types.put(INT.getName() + SUBFIX_VOLATILE, INT.volatileType());
                types.put(INT.getName() + SUBFIX_CV, INT.constVolatileType());

                types.put(FLOAT.getName(), FLOAT);
                types.put(FLOAT.getName() + SUBFIX_CONST, FLOAT.constType());
                types.put(FLOAT.getName() + SUBFIX_VOLATILE, FLOAT.volatileType());
                types.put(FLOAT.getName() + SUBFIX_CV, FLOAT.constVolatileType());

                types.put(DOUBLE.getName(), DOUBLE);
                types.put(DOUBLE.getName() + SUBFIX_CONST, DOUBLE.constType());
                types.put(DOUBLE.getName() + SUBFIX_VOLATILE, DOUBLE.volatileType());
                types.put(DOUBLE.getName() + SUBFIX_CV, DOUBLE.constVolatileType());

                types.put(SHORT.getName(), SHORT);
                types.put(SHORT.getName() + SUBFIX_CONST, SHORT.constType());
                types.put(SHORT.getName() + SUBFIX_VOLATILE, SHORT.volatileType());
                types.put(SHORT.getName() + SUBFIX_CV, SHORT.constVolatileType());

                types.put(LONG.getName(), LONG);
                types.put(LONG.getName() + SUBFIX_CONST, LONG.constType());
                types.put(LONG.getName() + SUBFIX_VOLATILE, LONG.volatileType());
                types.put(LONG.getName() + SUBFIX_CV, LONG.constVolatileType());

                types.put(LONGLONG.getName(), LONGLONG);
                types.put(LONGLONG.getName() + SUBFIX_CONST, LONGLONG.constType());
                types.put(LONGLONG.getName() + SUBFIX_VOLATILE, LONGLONG.volatileType());
                types.put(LONGLONG.getName() + SUBFIX_CV, LONGLONG.constVolatileType());

                types.put(UNSIGNED_INT.getName(), UNSIGNED_INT);
                types.put(UNSIGNED_INT.getName() + SUBFIX_CONST, UNSIGNED_INT.constType());
                types.put(UNSIGNED_INT.getName() + SUBFIX_VOLATILE, UNSIGNED_INT.volatileType());
                types.put(UNSIGNED_INT.getName() + SUBFIX_CV, UNSIGNED_INT.constVolatileType());

                types.put(UNSIGNED_CHAR.getName(), UNSIGNED_CHAR);
                types.put(UNSIGNED_CHAR.getName() + SUBFIX_CONST, UNSIGNED_CHAR.constType());
                types.put(UNSIGNED_CHAR.getName() + SUBFIX_VOLATILE, UNSIGNED_CHAR.volatileType());
                types.put(UNSIGNED_CHAR.getName() + SUBFIX_CV, UNSIGNED_CHAR.constVolatileType());

                types.put(UNSIGNED_SHORT.getName(), UNSIGNED_SHORT);
                types.put(UNSIGNED_SHORT.getName() + SUBFIX_CONST, UNSIGNED_SHORT.constType());
                types.put(UNSIGNED_SHORT.getName() + SUBFIX_VOLATILE, UNSIGNED_SHORT.volatileType());
                types.put(UNSIGNED_SHORT.getName() + SUBFIX_CV, UNSIGNED_SHORT.constVolatileType());

                types.put(UNSIGNED_LONG.getName(), UNSIGNED_LONG);
                types.put(UNSIGNED_LONG.getName() + SUBFIX_CONST, UNSIGNED_LONG.constType());
                types.put(UNSIGNED_LONG.getName() + SUBFIX_VOLATILE, UNSIGNED_LONG.volatileType());
                types.put(UNSIGNED_LONG.getName() + SUBFIX_CV, UNSIGNED_LONG.constVolatileType());

                types.put(UNSIGNED_LONGLONG.getName(), UNSIGNED_LONGLONG);
                types.put(UNSIGNED_LONGLONG.getName() + SUBFIX_CONST, UNSIGNED_LONGLONG.constType());
                types.put(UNSIGNED_LONGLONG.getName() + SUBFIX_VOLATILE, UNSIGNED_LONGLONG.volatileType());
                types.put(UNSIGNED_LONGLONG.getName() + SUBFIX_CV, UNSIGNED_LONGLONG.constVolatileType());

                types.put(BOOL.getName(), BOOL);
                types.put(BOOL.getName() + SUBFIX_CONST, BOOL.constType());
                types.put(BOOL.getName() + SUBFIX_VOLATILE, BOOL.volatileType());
                types.put(BOOL.getName() + SUBFIX_CV, BOOL.constVolatileType());

                types.put(VOID.getName(), VOID);
                types.put(VOID.getName() + SUBFIX_CONST, VOID.constType());
                types.put(VOID.getName() + SUBFIX_VOLATILE, VOID.volatileType());
                types.put(VOID.getName() + SUBFIX_CV, VOID.constVolatileType());

                types.put(SIZE_T.getName(), SIZE_T);
                types.put(SIZE_T.getName() + SUBFIX_CONST, SIZE_T.constType());
                types.put(SIZE_T.getName() + SUBFIX_VOLATILE, SIZE_T.volatileType());
                types.put(SIZE_T.getName() + SUBFIX_CV, SIZE_T.constVolatileType());

                types.put(SSIZE_T.getName(), SSIZE_T);
                types.put(SSIZE_T.getName() + SUBFIX_CONST, SSIZE_T.constType());
                types.put(SSIZE_T.getName() + SUBFIX_VOLATILE, SSIZE_T.volatileType());
                types.put(SSIZE_T.getName() + SUBFIX_CV, SSIZE_T.constVolatileType());

                types.put(TIME_T.getName(), TIME_T);
                types.put(TIME_T.getName() + SUBFIX_CONST, TIME_T.constType());
                types.put(TIME_T.getName() + SUBFIX_VOLATILE, TIME_T.volatileType());
                types.put(TIME_T.getName() + SUBFIX_CV, TIME_T.constVolatileType());

            }
        }
        return types.getOrDefault(name, null);
    }

    @Override
    public <T extends AbstractNativeType> T copy() {
        return (T)this;
    }
}
