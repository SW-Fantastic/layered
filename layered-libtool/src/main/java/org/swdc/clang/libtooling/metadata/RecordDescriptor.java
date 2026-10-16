package org.swdc.clang.libtooling.metadata;

import java.util.Map;

public class RecordDescriptor extends AbstractDescriptor {

    private Map<String,String> fields;

    /**
     * 是否支持移动构造
     */
    private boolean movable;

    /**
     * 是否具备无参数构造函数
     */
    private boolean hasNoArgsCtor;

    /**
     * 是否为完整类型，不完整的类型不能生成构造方法和析构方法。
     */
    private boolean completeType;

    public boolean isMovable() {
        return movable;
    }

    public void setMovable(boolean movable) {
        this.movable = movable;
    }

    public boolean isHasNoArgsCtor() {
        return hasNoArgsCtor;
    }

    public void setHasNoArgsCtor(boolean hasNoArgsCtor) {
        this.hasNoArgsCtor = hasNoArgsCtor;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public boolean isCompleteType() {
        return completeType;
    }

    public void setCompleteType(boolean completeType) {
        this.completeType = completeType;
    }
}
