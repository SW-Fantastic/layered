package org.swdc.layer.test.pointers;

public interface PdfiumDoc {

    void FPDF_InitLibrary();

    void FPDF_DestroyLibrary();

    PDFDocument FPDF_LoadDocument(String filename, String password);

    void FPDF_CloseDocument(PDFDocument document);

    long FPDF_GetLastError();

}
