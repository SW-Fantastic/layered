package org.swdc.clang.framework;

public class ParseExceptionElement {

    private String location;

    private String message;

    public ParseExceptionElement(String location, String message) {
        this.location = location;
        this.message = message;
    }

    public String getLocation() {
        return location;
    }

    public String getMessage() {
        return message;
    }
}
