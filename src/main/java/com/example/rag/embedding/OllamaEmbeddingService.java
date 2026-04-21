package com.example.rag.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OllamaEmbeddingService {
     private final String baseUrl;
     private final String model;
     private final HttpClient httpClient;
     private final ObjectMapper objectMapper;

     public OllamaEmbeddingService(String baseUrl, String model) {
          this.baseUrl = baseUrl;
          this.model = model;
          this.httpClient = HttpClient.newHttpClient();
          this.objectMapper = new ObjectMapper();
     }

     public OllamaEmbeddingService() {
          this(
                    com.example.rag.config.Config.OLLAMA_BASE_URL,
                    com.example.rag.config.Config.OLLAMA_EMBEDDING_MODEL);
     }

     public float[] generateEmbedding(String text) throws Exception {
          String endpoint = baseUrl + "/api/embeddings";
          Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "prompt", text);

          String jsonBody = objectMapper.writeValueAsString(requestBody);

          int maxAttempts = 3;
          Exception lastEx = null;
          for (int attempt = 1; attempt <= maxAttempts; attempt++) {
               try {
                    HttpRequest request = HttpRequest.newBuilder()
                              .uri(URI.create(endpoint))
                              .header("Content-Type", "application/json")
                              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                              .build();

                    HttpResponse<String> response = httpClient.send(request,
                              HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() != 200) {
                         lastEx = new RuntimeException(
                                   "Ollama API error: " + response.statusCode() + " - " + response.body());
                         // retry
                         Thread.sleep(200L * attempt);
                         continue;
                    }

                    JsonNode jsonResponse = objectMapper.readTree(response.body());
                    JsonNode embeddingNode = jsonResponse.get("embedding");

                    if (embeddingNode == null || !embeddingNode.isArray()) {
                         throw new RuntimeException("Invalid embedding response from Ollama");
                    }

                    float[] embedding = new float[embeddingNode.size()];
                    for (int i = 0; i < embeddingNode.size(); i++) {
                         embedding[i] = (float) embeddingNode.get(i).asDouble();
                    }

                    // Validate embedding dimension
                    if (embedding.length != com.example.rag.config.Config.EMBEDDING_DIM) {
                         throw new RuntimeException(
                                   "Embedding dimension " + embedding.length + " does not match expected "
                                             + com.example.rag.config.Config.EMBEDDING_DIM);
                    }

                    return embedding;
               } catch (Exception e) {
                    lastEx = e;
                    if (attempt < maxAttempts) {
                         Thread.sleep(200L * attempt);
                         continue;
                    }
               }
          }

          throw lastEx == null ? new RuntimeException("Unknown error generating embedding") : lastEx;
     }

     public List<float[]> generateBatchEmbeddings(List<String> texts) throws Exception {
          List<float[]> embeddings = new ArrayList<>();
          for (String text : texts) {
               embeddings.add(generateEmbedding(text));
          }
          return embeddings;
     }

     public boolean isAvailable() {
          try {
               HttpRequest request = HttpRequest.newBuilder()
                         .uri(URI.create(baseUrl + "/api/tags"))
                         .GET()
                         .build();

               HttpResponse<String> response = httpClient.send(request,
                         HttpResponse.BodyHandlers.ofString());

               return response.statusCode() == 200;
          } catch (Exception e) {
               return false;
          }
     }

     public List<String> listModels() throws Exception {
          HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .GET()
                    .build();

          HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

          JsonNode jsonResponse = objectMapper.readTree(response.body());
          JsonNode modelsNode = jsonResponse.get("models");

          List<String> models = new ArrayList<>();
          if (modelsNode != null && modelsNode.isArray()) {
               for (JsonNode modelNode : modelsNode) {
                    models.add(modelNode.get("name").asText());
               }
          }

          return models;
     }
}
