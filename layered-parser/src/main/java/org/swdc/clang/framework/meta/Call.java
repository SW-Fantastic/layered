package org.swdc.clang.framework.meta;

/**
 * 生成过程中的函数对象。
 */
public class Call {

    public static final String METHOD_PREFIX = "_func";

    private long index;

    private String mangled;

    private String mangledReturnType;

    private String bodySource;

    private String returnTypeSource;

    private String parametersSource;

    public String getMangled() {
        return mangled;
    }

    public void setMangled(String mangled) {
        this.mangled = mangled;
    }

    public void setMangledReturnType(String mangledReturnType) {
        this.mangledReturnType = mangledReturnType;
    }

    public String getMangledReturnType() {
        return mangledReturnType;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public String getBodySource() {
        return bodySource;
    }

    public void setBodySource(String bodySource) {
        this.bodySource = bodySource;
    }

    public String getParametersSource() {
        return parametersSource;
    }

    public void setParametersSource(String parametersSource) {
        this.parametersSource = parametersSource;
    }

    public String getReturnTypeSource() {
        return returnTypeSource;
    }

    public void setReturnTypeSource(String returnTypeSource) {
        this.returnTypeSource = returnTypeSource;
    }

    public String getIndexedName() {
        return METHOD_PREFIX + index;
    }

    public String createSource() {
        return returnTypeSource + " " + getIndexedName() + parametersSource + " {\r\n" + bodySource + "\r\n} \r\n";
    }

    public String createHeaderSource(String prefix, String macroExport) {
        String sig = returnTypeSource + " " + macroExport + " " + getIndexedName() + parametersSource + ";";
        return prefix + sig + "\r\n";
    }

}
