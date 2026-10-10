package org.swdc.layered;

public enum MetaType {

    INT("I", "int"),
    LONG("L", "long"),
    FLOAT("F", "float"),
    DOUBLE("D", "double"),
    SHORT("S", "short"),
    CHAR("C", "char"),
    VOID("V", "void"),
    SIZE_T("Sz", "size_t"),

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
