package org.swdc.layered.plugin;

import java.util.List;

public class PlatformSpecified {

    private List<String> includeSearchDirs;

    private List<String> librarySearchDirs;

    private List<String> headers;

    private List<String> libraries;

    private List<String> clangParams;

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<String> getClangParams() {
        return clangParams;
    }

    public void setClangParams(List<String> clangParams) {
        this.clangParams = clangParams;
    }

    public List<String> getIncludeSearchDirs() {
        return includeSearchDirs;
    }

    public void setIncludeSearchDirs(List<String> includeSearchDirs) {
        this.includeSearchDirs = includeSearchDirs;
    }

    public List<String> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<String> libraries) {
        this.libraries = libraries;
    }

    public List<String> getLibrarySearchDirs() {
        return librarySearchDirs;
    }

    public void setLibrarySearchDirs(List<String> librarySearchDirs) {
        this.librarySearchDirs = librarySearchDirs;
    }


}
