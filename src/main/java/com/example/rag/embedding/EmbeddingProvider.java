package com.example.rag.embedding;

public interface EmbeddingProvider {
     float[] embed(String text) throws Exception;
}
