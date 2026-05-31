package org.swdc.layered.module;

public class LayerLibrary extends AbstractLayerLibrary {

    private static final LayerLibrary instance = new LayerLibrary();

    private LayerLibrary() {

    }

    @Override
    public String getLibraryName() {
        return "layered-runtime";
    }

    public static LayerLibrary getInstance() {
        return instance;
    }
}
