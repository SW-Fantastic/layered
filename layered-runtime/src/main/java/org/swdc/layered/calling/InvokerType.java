package org.swdc.layered.calling;

public enum InvokerType {

    LAYER_ADDRESS(0),
    LAYER_INT(1),
    LAYER_UINT(2),
    LAYER_LONG(3),
    LAYER_ULONG(4),
    LAYER_FLOAT(5),
    LAYER_DOUBLE(6),
    LAYER_CHAR(7),
    LAYER_UCHAR(8),
    LAYER_BOOL(9),
    LAYER_VOID(10);

    private final int layerTypeFlag;

    InvokerType(int layerTypeFlag) {
        this.layerTypeFlag = layerTypeFlag;
    }

    public int getFlag() {
        return layerTypeFlag;
    }
}
