package cr.ac.una.restunaclient.util;

import org.apache.pdfbox.Loader;                 // <-- NUEVO
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPageable;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

public final class PdfPrinter {
    private PdfPrinter(){}

    /** Imprime bytes PDF a la impresora predeterminada. */
    public static void print(byte[] pdfBytes) throws Exception {
        if (pdfBytes == null || pdfBytes.length == 0)
            throw new IllegalArgumentException("PDF vacío.");
        // PDFBox 3: usar Loader.loadPDF(...)
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {     // <-- CAMBIO
            doPrint(doc);
        }
    }

    /** Imprime un archivo PDF a la impresora predeterminada. */
    public static void print(File pdfFile) throws Exception {
        if (pdfFile == null || !pdfFile.exists())
            throw new IllegalArgumentException("Archivo PDF no encontrado.");
        // Puedes cargar directo desde File con Loader
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {      // <-- CAMBIO
            doPrint(doc);
        }
        // (si prefieres seguir con bytes, también sirve:)
        // byte[] bytes = Files.readAllBytes(pdfFile.toPath());
        // print(bytes);
    }

    private static void doPrint(PDDocument doc) throws Exception {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            PrintService def = PrintServiceLookup.lookupDefaultPrintService();
            if (def != null) job.setPrintService(def);
            job.setPageable(new PDFPageable(doc));
            job.print(new HashPrintRequestAttributeSet());
        } catch (PrinterException e) {
            throw new Exception("No hay impresora predeterminada o no se pudo imprimir. " + e.getMessage(), e);
        }
    }
}