package com.example.rag.ingestion;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.rag.embedding.OllamaEmbeddingService;
import com.example.rag.vectorstore.VectorStore;

public class DocumentIngestionService {

    public static void ingestFolder(String folderPath,
                                    VectorStore vectorStore,
                                    OllamaEmbeddingService embeddingService) throws Exception {

        File folder = new File(folderPath);
        File[] files = folder.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("No files found in folder: " + folderPath);
            return;
        }

        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }

            System.out.println("\nProcessing file: " + file.getName());

            String content = DocumentReaderService.readFile(file);

            if (content == null || content.isBlank()) {
                System.out.println("Skipping empty file: " + file.getName());
                continue;
            }

            List<String> chunks = ChunkingService.chunkText(content, 500);
            int chunkCount = 0;

            // Replace any prior vectors for the same source file.
            vectorStore.deleteBySource(file.getName());

            for (String chunk : chunks) {
                if (chunk == null || chunk.trim().isEmpty()) {
                    continue;
                }

                float[] embedding = embeddingService.generateEmbedding(chunk);
                String id = buildChunkId(file.getName(), chunkCount, chunk);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", file.getName());
                metadata.put("documentType", getDocType(file.getName()));
                metadata.put("chunkIndex", chunkCount);
                metadata.put("folderPath", folderPath);

                vectorStore.upsert(id, chunk, embedding, metadata);

                System.out.println("Inserted chunk " + chunkCount + " -> " + id);
                chunkCount++;
            }

            System.out.println("Finished file: " + file.getName() + " | Total chunks: " + chunkCount);
        }

        System.out.println("\nIngestion completed for folder: " + folderPath);
    }

    private static String buildChunkId(String fileName, int chunkIndex, String chunk) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest((fileName + "|" + chunkIndex + "|" + chunk).getBytes(StandardCharsets.UTF_8));

        StringBuilder builder = new StringBuilder("doc-");
        for (int i = 0; i < 8 && i < hash.length; i++) {
            builder.append(String.format("%02x", hash[i]));
        }
        return builder.toString();
    }

    private static String getDocType(String fileName) {
        String lower = fileName.toLowerCase();

        if (lower.contains("contract")) return "contract";
        if (lower.contains("invoice")) return "invoice";
        if (lower.contains("policy")) return "policy";
        if (lower.contains("mail") || lower.contains("log")) return "log";

        return "unknown";
    }
}
