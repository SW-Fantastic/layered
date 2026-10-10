package org.swdc.layered.library;

import java.io.File;

public class LayerRuntimeLibrary {

    private static LayerRuntimeLibrary INSTANCE = new LayerRuntimeLibrary();

    private LayerLibrary runtimeLibrary;

    private LayerRuntimeLibrary() {
        this.runtimeLibrary = new LayerLibrary("layeredRuntime");
    }

    public void load(File resourceFolder) {
        try {
            this.runtimeLibrary.resolve(LayerLibrary.class,resourceFolder, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static LayerRuntimeLibrary getInstance() {
        return INSTANCE;
    }
}
