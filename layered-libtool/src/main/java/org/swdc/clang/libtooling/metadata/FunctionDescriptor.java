package org.swdc.clang.libtooling.metadata;

import java.util.ArrayList;
import java.util.List;

public class FunctionDescriptor extends AbstractDescriptor {

    private String returnTypeId;

    private int paramCount;

    private List<String> paramTypeIds = new ArrayList<>();

    private boolean callback;

    public boolean isCallback() {
        return callback;
    }

    public void setCallback(boolean callback) {
        this.callback = callback;
    }

    public int getParamCount() {
        return paramCount;
    }

    public void setParamCount(int paramCount) {
        this.paramCount = paramCount;
    }

    public String getReturnTypeId() {
        return returnTypeId;
    }

    public void setReturnTypeId(String returnTypeId) {
        this.returnTypeId = returnTypeId;
    }

    public List<String> getParamTypeIds() {
        return paramTypeIds;
    }

    public void setParamTypeIds(List<String> paramTypeIds) {
        this.paramTypeIds = paramTypeIds;
    }

}
