package org.swdc.layered.def;


public class WritableParameter {

    /**
     * 参数的index
     */
    private int index;

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数的本地类型
     */
    private NativeDefinition type;

    /**
     * 参数的callback metadata（如果参数是一个函数指针）
     */
    private WritableFunction callback;

    /**
     * 参数的深度（指针的层数，数组的长度）
     */
    private long nested;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public NativeDefinition getType() {
        return type;
    }

    public void setType(NativeDefinition type) {
        this.type = type;
    }

    public long getNested() {
        return nested;
    }

    public void setNested(long nested) {
        this.nested = nested;
    }

    public void setCallback(WritableFunction callback) {
        this.callback = callback;
    }

    public WritableFunction getCallback() {
        return callback;
    }

    public static WritableParameter create(int index, String name, NativeDefinition type) {
        return create(index,name, null,type, 0);
    }

    public static WritableParameter create(int index, String name,WritableFunction callback,NativeDefinition type) {
        return create(index,name, callback,type, 0);
    }

    public static WritableParameter create(int index, String name,WritableFunction callback, NativeDefinition type, long nested) {
        WritableParameter param = new WritableParameter();
        param.setIndex(index);
        param.setName(name);
        param.setType(type);
        param.setCallback(callback);
        param.setNested(nested);
        return param;
    }


}
