package com.example.rag.embedding;

import com.example.rag.config.Config;

public class OpenAIEmbedding implements EmbeddingProvider {

     @Override
     public float[] embed(String text) {
          // Placeholder (normally call OpenAI / Gemini embedding API)
          return new float[Config.EMBEDDING_DIM];
     }
}
