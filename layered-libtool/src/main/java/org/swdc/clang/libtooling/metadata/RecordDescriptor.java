package org.swdc.clang.libtooling.metadata;

import java.util.Map;

public class RecordDescriptor extends AbstractDescriptor {

    private Map<String,String> fields;

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

}
