package org.swdc.clang.libtooling.generator.sources;

import java.util.*;

public class RenderableSource {

    private final String prefix;

    private final Map<String, String> sources = new HashMap<>();

    private final Map<String, String> declares = new HashMap<>();

    private final String includePath;

    private final List<String> includes = new ArrayList<>();

    public RenderableSource(String prefix, String includeRelativePath) {
        this.prefix = prefix;
        this.includePath = includeRelativePath;
    }

    public void addInclude(String includePath) {
        this.includes.add(includePath);
    }

    public List<String> getIncludes() {
        return Collections.unmodifiableList(includes);
    }

    public String getIncludePath() {
        return includePath;
    }

    public String getPrefix() {
        return prefix;
    }

    public Map<String, String> getDeclares() {
        return declares;
    }

    public Map<String, String> getSources() {
        return sources;
    }

    public void addSource(String mangledName, String declare, String source) {
        sources.put(mangledName, source);
        declares.put(mangledName, declare);
    }

}
