package com.example.rag.ingestion;

import java.util.ArrayList;
import java.util.List;

public class ChunkingService {

    public static List<String> chunkText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }

        return chunks;
    }
}