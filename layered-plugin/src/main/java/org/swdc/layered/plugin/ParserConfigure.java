package org.swdc.layered.plugin;

import java.util.List;
import java.util.Map;

public class ParserConfigure {

    /**
     * Include目录
     */
    private List<String> includeDirs;

    /**
     * 头文件名称
     */
    private List<String> headers;

    /**
     * 解析参数（会传递给libclang解析器）
     */
    private List<String> parameters;

    /**
     * 链接库表，key - 链接库的名称，value - 链接库的相对路径。
     */
    private Map<String, String> libraries;

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<String> getIncludeDirs() {
        return includeDirs;
    }

    public void setIncludeDirs(List<String> includeDirs) {
        this.includeDirs = includeDirs;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setLibraries(Map<String, String> libraries) {
        this.libraries = libraries;
    }

    public Map<String, String> getLibraries() {
        return libraries;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

}
