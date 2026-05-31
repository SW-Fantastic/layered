package org.swdc.clang.framework;

import java.util.ArrayList;
import java.util.List;

public class ParseException extends Exception {

    private List<ParseExceptionElement> elements = new ArrayList<>();

    public ParseException(String message) {
        super(message);
    }

    public void addElement(ParseExceptionElement element) {
        elements.add(element);
    }

    public List<ParseExceptionElement> getElements() {
        return elements;
    }

    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        for (ParseExceptionElement element : elements) {
            sb.append(element.getLocation()).append(": ").append(element.getMessage()).append("\n");
        }
        return sb.toString();
    }

}
