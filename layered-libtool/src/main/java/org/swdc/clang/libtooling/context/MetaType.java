package org.swdc.clang.libtooling.context;

public enum MetaType {

    INT("I", "int"),
    LONG("L", "long"),
    FLOAT("F", "float"),
    DOUBLE("D", "double"),
    SHORT("S", "short"),
    CHAR("C", "char"),
    VOID("V", "void"),

    INTPTR_T("p", "intptr_t");

    private String mangledFlag;

    private String typeName;

    MetaType(String mangledFlag, String typeName) {
        this.mangledFlag = mangledFlag;
        this.typeName = typeName;
    }

    public String getMangledFlag() {
        return mangledFlag;
    }

    public String getTypeName() {
        return typeName;
    }
}
