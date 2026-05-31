package org.swdc.clang.framework.source;

import org.swdc.clang.framework.meta.Call;
import org.swdc.layered.def.WritableFunction;

import java.util.HashMap;
import java.util.Map;

public class SourceContext {

    private Map<String, Call> calls = new HashMap<>();

    private Map<String, WritableFunction> metadata = new HashMap<>();

    private SourceGenerate generate;

    SourceContext(SourceGenerate generate) {
       this.generate = generate;
    }

    public synchronized void addCall(String name, WritableFunction function, Call call) {
        call.setIndex(generate.getIndex());
        calls.put(name + "@" + call.getMangled(), call);
        metadata.put("(" + call.getMangledReturnType() + ")" + name + "@" + call.getMangled(), function);
        function.setSymbolName(call.getIndexedName());
    }


    public String createSource() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Call> entry : calls.entrySet()) {
            sb.append(entry.getValue().createSource()).append("\r\n");
        }
        return sb.toString();
    }

    public String createHeaderSource(String exportMacro) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Call> entry : calls.entrySet()) {
            sb.append(entry.getValue().createHeaderSource("\t",exportMacro)).append("\r\n");
        }
        return sb.toString();
    }

    public Map<String, WritableFunction> getMetadata() {
        return metadata;
    }

}
