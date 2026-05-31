package org.swdc.layer.test.pointers;

import org.swdc.layered.pointers.Allocator;
import org.swdc.layered.pointers.ObjectivePointer;


public class PDFDocument extends ObjectivePointer {

    protected PDFDocument(Allocator allocator, long address, boolean owner) {
        super(allocator,address,owner);
    }


}
