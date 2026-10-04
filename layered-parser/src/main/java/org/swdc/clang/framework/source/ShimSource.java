package org.swdc.clang.framework.source;

import java.util.ArrayList;
import java.util.List;

public class ShimSource {

    private List<String> templateDecl = new ArrayList<>();

    public void addTemplateDecl(String decl) {
        if (decl == null || decl.isBlank()) {
            return;
        }
        decl = decl.trim();
        if (templateDecl.contains(decl)) {
            return;
        }
        templateDecl.add(decl);
    }

    public String createSource(String header) {
        StringBuilder source = new StringBuilder();
        source.append("#ifndef _SHIM_H_")
                .append("#include \"").append(header).append("\"")
                .append("#define SHIM_H_");
        for (String className : templateDecl) {
            source.append("template class ").append(className);
        }
        source.append("#endif");
        return source.toString();
    }

}
