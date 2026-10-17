package org.swdc.layered.library;

@FunctionalInterface
public interface LayerExceptionHandler {

    void handle(Throwable t);

}
