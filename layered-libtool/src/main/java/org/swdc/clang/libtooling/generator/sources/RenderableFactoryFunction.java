package org.swdc.clang.libtooling.generator.sources;


import org.swdc.clang.libtooling.context.RecordMetaType;

import java.util.ArrayList;
import java.util.List;

public class RenderableFactoryFunction {

    private List<RenderableFunction> indexedCtors = new ArrayList<>();

    private RecordMetaType metaType;

    private int dtorIndex;

    private String dtorPrefix;

    public RenderableFactoryFunction(RecordMetaType metaType, int dtorIndex, String dtorPrefix) {
        this.metaType = metaType;
        this.dtorIndex = dtorIndex;
        this.dtorPrefix = dtorPrefix;
    }

    public void addCtor(RenderableFunction renderableFunction) {
        indexedCtors.add(renderableFunction);
    }

    public List<RenderableFunction> getCtors() {
        return indexedCtors;
    }

    public int getDtorIndex() {
        return dtorIndex;
    }

    public RecordMetaType getMetaType() {
        return metaType;
    }

    public String getDtorPrefix() {
        return dtorPrefix;
    }
}
