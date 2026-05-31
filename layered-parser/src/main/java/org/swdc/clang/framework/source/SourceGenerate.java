package org.swdc.clang.framework.source;

public class SourceGenerate {

    private volatile int index = 0;

    public synchronized int getIndex() {
        index++;
        return index;
    }

    public SourceContext createContext() {
        return new SourceContext(this);
    }

}
