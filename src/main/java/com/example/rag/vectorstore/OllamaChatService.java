package com.example.rag.vectorstore;

import com.example.rag.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class OllamaChatService {
     private final String baseUrl;
     private final String model;
     private final HttpClient httpClient;
     private final ObjectMapper objectMapper;

     public OllamaChatService(String baseUrl, String model) {
          this.baseUrl = baseUrl;
          this.model = model;
          this.httpClient = HttpClient.newHttpClient();
          this.objectMapper = new ObjectMapper();
     }

     public OllamaChatService() {
          this(Config.OLLAMA_BASE_URL, Config.OLLAMA_MODEL);
     }

     /**
      * Generate text using Ollama's /api/generate endpoint
      */
     public String generate(String prompt) throws Exception {
          String endpoint = baseUrl + "/api/generate";

          Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false);

          String jsonBody = objectMapper.writeValueAsString(requestBody);

          HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

          HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

          if (response.statusCode() != 200) {
               throw new RuntimeException("Ollama API error: " + response.statusCode());
          }

          String respBody = response.body();

          // Try parse JSON; tolerate quoted JSON strings and raw text
          try {
               JsonNode jsonResponse = objectMapper.readTree(respBody);
               if (jsonResponse.isTextual()) {
                    return jsonResponse.asText();
               }

               // safe extraction similar to OllamaLLM
               String text = null;
               if (jsonResponse.has("response") && !jsonResponse.get("response").isNull()) {
                    text = jsonResponse.get("response").asText();
               } else if (jsonResponse.has("results") && jsonResponse.get("results").isArray()
                         && jsonResponse.get("results").size() > 0) {
                    JsonNode first = jsonResponse.get("results").get(0);
                    if (first.has("content") && !first.get("content").isNull())
                         text = first.get("content").asText();
                    else
                         text = first.toString();
               } else if (jsonResponse.has("content") && !jsonResponse.get("content").isNull()) {
                    text = jsonResponse.get("content").asText();
               } else {
                    text = respBody;
               }

               return text == null ? "" : text;
          } catch (Exception e) {
               // If body is a quoted JSON string like "...\n...", try to unquote it
               try {
                    String unquoted = objectMapper.readValue(respBody, String.class);
                    return unquoted == null ? "" : unquoted;
               } catch (Exception ex) {
                    return respBody == null ? "" : respBody;
               }
          }
     }

     /**
      * Generate text with streaming support
      */
     public String generateStreaming(String prompt, StreamCallback callback) throws Exception {
          String endpoint = baseUrl + "/api/generate";

          Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", true);

          String jsonBody = objectMapper.writeValueAsString(requestBody);

          HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

          StringBuilder fullResponse = new StringBuilder();

          HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

          String[] lines = response.body().split("\n");
          for (String line : lines) {
               if (!line.trim().isEmpty()) {
                    String chunk = "";
                    try {
                         JsonNode jsonResponse = objectMapper.readTree(line);
                         if (jsonResponse.isTextual()) {
                              chunk = jsonResponse.asText();
                         } else if (jsonResponse.has("response") && !jsonResponse.get("response").isNull()) {
                              chunk = jsonResponse.get("response").asText();
                         } else if (jsonResponse.has("chunk") && !jsonResponse.get("chunk").isNull()) {
                              chunk = jsonResponse.get("chunk").asText();
                         } else if (jsonResponse.has("content") && !jsonResponse.get("content").isNull()) {
                              chunk = jsonResponse.get("content").asText();
                         } else {
                              chunk = jsonResponse.toString();
                         }

                         if (callback != null) {
                              callback.onChunk(chunk);
                         }

                         fullResponse.append(chunk);

                         if (jsonResponse.has("done") && jsonResponse.get("done").asBoolean()) {
                              break;
                         }
                    } catch (Exception e) {
                         // Line is not JSON; try to unquote if it's a quoted JSON string
                         try {
                              String unquoted = objectMapper.readValue(line, String.class);
                              chunk = unquoted == null ? "" : unquoted;
                         } catch (Exception ex) {
                              chunk = line;
                         }

                         if (callback != null) {
                              callback.onChunk(chunk);
                         }

                         fullResponse.append(chunk);
                    }
               }
          }

          return fullResponse.toString();
     }

     @FunctionalInterface
     public interface StreamCallback {
          void onChunk(String chunk);
     }
}
