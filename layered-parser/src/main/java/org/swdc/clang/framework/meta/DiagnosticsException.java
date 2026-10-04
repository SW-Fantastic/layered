package org.swdc.clang.framework.meta;

public class DiagnosticsException extends Exception {

    private String message;

    public DiagnosticsException(String message) {
        super(message);
    }

}
