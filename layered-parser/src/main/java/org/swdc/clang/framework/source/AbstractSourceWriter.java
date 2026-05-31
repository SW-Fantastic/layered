package org.swdc.clang.framework.source;

import org.swdc.clang.framework.meta.Call;

import java.util.List;

public abstract class AbstractSourceWriter<T> {

    public abstract void createCalls(SourceContext context, T type);

}
