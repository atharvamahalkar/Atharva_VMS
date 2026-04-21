package com.example.rag.integration;

import com.example.rag.embedding.OllamaEmbeddingService;
import com.example.rag.pipeline.RAGPipeline;
import com.example.rag.vectorstore.VectorStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class RAGIntegrationTest {

     @Test
     public void testKnowledgeUploadAndQuery() throws Exception {
          String pineconeKey = System.getenv("PINECONE_API_KEY");
          Assumptions.assumeTrue(pineconeKey != null && !pineconeKey.isBlank(),
                    "PINECONE_API_KEY not set - skipping integration test");

          OllamaEmbeddingService embSvc = new OllamaEmbeddingService();
          Assumptions.assumeTrue(embSvc.isAvailable(),
                    "Ollama embedding service not available - skipping integration test");

          VectorStore vs = new VectorStore();
          // upload knowledge2.txt (idempotent)
          vs.upsertFromFile("src/main/java/com/example/rag/knowledge2.txt", embSvc);

          RAGPipeline pipeline = new RAGPipeline();

          String answer = pipeline.ask("Summarize the knowledge file in one sentence.");

          Assertions.assertNotNull(answer, "Answer should not be null");
          Assertions.assertFalse(answer.isBlank(), "Answer should not be blank");

          String trimmed = answer.trim();
          // Should not be a JSON object
          Assertions.assertFalse(trimmed.startsWith("{") && trimmed.endsWith("}"), "Answer appears to be JSON object");
          // Should not be a quoted JSON string
          Assertions.assertFalse(trimmed.startsWith("\"") && trimmed.endsWith("\""),
                    "Answer appears to be a quoted JSON string");
          // Should not contain escaped newlines
          Assertions.assertFalse(answer.contains("\\n"), "Answer contains escaped newlines");
     }
}
