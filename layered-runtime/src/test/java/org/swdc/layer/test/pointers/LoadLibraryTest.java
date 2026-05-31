package org.swdc.layer.test.pointers;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.swdc.layered.def.PlatformModule;
import org.swdc.layered.def.WritableFunction;
import org.swdc.layered.module.LayerLibrary;
import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.pointers.BytePointer;

import java.io.File;
import java.util.Map;

public class LoadLibraryTest {

    private static Allocator allocator = null;

    @BeforeAll
    public static void  setup() {
        allocator = new Allocator();
        LayerLibrary library = LayerLibrary.getInstance();
        library.loadLibrary(new File("./assets"));
    }

    @Test
    public void testLoadLibrary() {

        PlatformModule module = PlatformModule.load(allocator,new File("libpdfium4j.dll"));
        Assertions.assertNotNull(module);
        Assertions.assertNotNull(module.getSymbolTable());

        for (Map.Entry<String, WritableFunction> items : module.getSymbolTable().entrySet()) {
            System.out.println(items.getKey());
        }

        module.free();
    }

    @Test
    public void testSymbolLoad() {

        PlatformModule module = PlatformModule.load(allocator,new File("libpdfium4j.dll"));
        PdfiumDoc doc = module.createCallProxy(PdfiumDoc.class);
        doc.FPDF_InitLibrary();
        PDFDocument document = doc.FPDF_LoadDocument(new File("test.pdf").getAbsolutePath(), null);
        doc.FPDF_CloseDocument(document);
        //doc.FPDF_GetPageHeightF(new OpaquePointer());

    }

}
