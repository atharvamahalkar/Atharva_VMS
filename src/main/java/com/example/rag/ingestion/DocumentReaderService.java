package com.example.rag.ingestion;

import java.io.File;
import java.nio.file.Files;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class DocumentReaderService {

    public static String readFile(File file) throws Exception {
        String name = file.getName().toLowerCase();

        if (name.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        }

        return Files.readString(file.toPath());
    }
}