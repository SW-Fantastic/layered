package org.swdc.layer.test.pointers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.swdc.layered.module.LayerLibrary;

import java.io.File;

public class PackageLoadLibrary {

    @Test
    public void load() {
        LayerLibrary layerLibrary = LayerLibrary.getInstance();
        layerLibrary.loadLibrary(new File("./assets"));
    }

}
