package com.example.rag.embedding;

import com.example.rag.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.time.Duration;
import java.util.Map;

public class OllamaEmbedding implements EmbeddingProvider {

    private static final ObjectMapper mapper = new ObjectMapper();

    // 🔥 LONG TIMEOUTS FOR CPU MODELS
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(120))
            .writeTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public float[] embed(String text) throws Exception {

        Map<String, Object> payloadMap = Map.of(
                "model", Config.OLLAMA_EMBEDDING_MODEL,
                "prompt", text);
        String payload = mapper.writeValueAsString(payloadMap);

        Request request = new Request.Builder()
                .url(Config.OLLAMA_BASE_URL + "/api/embeddings")
                .post(RequestBody.create(
                        payload, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                String errorBody = response.body() == null ? "" : response.body().string();
                throw new RuntimeException("Ollama error: " + errorBody);
            }

            String responseBody = response.body() == null ? "" : response.body().string();
            JsonNode root = mapper.readTree(responseBody);
            float[] embedding = mapper.convertValue(root.get("embedding"), float[].class);
            if (embedding.length != com.example.rag.config.Config.EMBEDDING_DIM) {
                throw new RuntimeException("Embedding dimension " + embedding.length + " does not match expected "
                        + com.example.rag.config.Config.EMBEDDING_DIM);
            }
            return embedding;
        }
    }
}
