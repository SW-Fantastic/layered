package org.swdc.clang.libtooling.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AbstractDescriptor {

    @JsonProperty("const")
    private Boolean isConst;

    @JsonProperty("volatile")
    private Boolean isVolatile;

    private String id;

    private String namespace;

    private String name;

    private DescriptorType type;

    public String getName() {
        return name;
    }

    public Boolean getConst() {
        return isConst;
    }

    public Boolean getVolatile() {
        return isVolatile;
    }

    public DescriptorType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setConst(Boolean aConst) {
        isConst = aConst;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setType(DescriptorType type) {
        this.type = type;
    }

    public void setVolatile(Boolean aVolatile) {
        isVolatile = aVolatile;
    }


}
