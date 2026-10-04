package org.swdc.clang.libtooling.generator.sources;

import java.util.HashMap;
import java.util.Map;

public class RenderableSource {

    private final String prefix;

    private final Map<String, String> sources = new HashMap<>();

    private final Map<String, String> declares = new HashMap<>();

    private final String includePath;

    public RenderableSource(String prefix, String includeRelativePath) {
        this.prefix = prefix;
        this.includePath = includeRelativePath;
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
