package org.swdc.clang.libtooling.generator.sources;

import org.swdc.clang.libtooling.context.AbstractMetaType;
import org.swdc.clang.libtooling.context.RecordMetaType;

public class RenderableField {

    private final RecordMetaType recordMetaType;

    private final AbstractMetaType fieldType;

    private final String fieldName;

    private final String prefix;

    private final int getterIndex;

    private final int setterIndex;

    public RenderableField(RecordMetaType recordMetaType, AbstractMetaType fieldType, String fieldName, String prefix, int getterIndex, int setterIndex) {
        this.recordMetaType = recordMetaType;
        this.fieldName = fieldName;
        this.prefix = prefix;
        this.getterIndex = getterIndex;
        this.setterIndex = setterIndex;
        this.fieldType = fieldType;
    }

    public RecordMetaType getRecordMetaType() {
        return recordMetaType;
    }

    public AbstractMetaType getFieldType() {
        return fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getGetterIndex() {
        return getterIndex;
    }

    public int getSetterIndex() {
        return setterIndex;
    }

}
