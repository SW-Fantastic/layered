package org.swdc.clang.libtooling.metadata;

public class BuiltInDescriptor extends AbstractDescriptor {

    private String symbol;

    public boolean isUnsigned() {
        return getName().toLowerCase().contains("unsigned");
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

}
