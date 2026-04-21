package com.example.rag.llm;

import java.util.Map;

import com.example.rag.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

public class OllamaLLM implements LLMProvider {

     private static final OkHttpClient client = new OkHttpClient();
     private static final ObjectMapper mapper = new ObjectMapper();

     @Override
     public String generate(String prompt) throws Exception {

          // String payload = """
          // {
          // "model": "%s",
          // "prompt": "%s",
          // "stream": false
          // }
          // """.formatted(Config.OLLAMA_MODEL, prompt);

          // Request request = new Request.Builder()
          // .url("http://localhost:11434/api/generate")
          // .post(RequestBody.create(
          // payload, MediaType.parse("application/json")))
          // .build();

          // Response response = client.newCall(request).execute();
          // String respBody = response.body().string();
          // JsonNode root = mapper.readTree(respBody);

          // Safely extract text from possible response shapes
          // Build payload using ObjectMapper to safely escape prompt
          Map<String, Object> payloadMap = Map.of(
                    "model", Config.OLLAMA_MODEL,
                    "prompt", prompt,
                    "stream", false);

          String payload = mapper.writeValueAsString(payloadMap);

          Request request = new Request.Builder()
                    .url(Config.OLLAMA_BASE_URL + "/api/generate")
                    .post(RequestBody.create(
                              payload, MediaType.parse("application/json")))
                    .build();

          int maxAttempts = 3;
          Exception lastEx = null;
          String respBody = null;
          for (int attempt = 1; attempt <= maxAttempts; attempt++) {
               try (Response response = client.newCall(request).execute()) {
                    respBody = response.body().string();
                    if (response.code() != 200) {
                         lastEx = new RuntimeException("Ollama API error: " + response.code() + " - " + respBody);
                         Thread.sleep(200L * attempt);
                         continue;
                    }
                    // success
                    lastEx = null;
                    break;
               } catch (Exception e) {
                    lastEx = e;
                    if (attempt < maxAttempts) {
                         Thread.sleep(200L * attempt);
                         continue;
                    }
               }
          }

          if (lastEx != null && respBody == null) {
               throw lastEx;
          }

          // Try to parse JSON; if not JSON, attempt to unquote quoted JSON string, then
          // return raw body
          JsonNode root = null;
          try {
               root = mapper.readTree(respBody);
               if (root.isTextual()) {
                    return root.asText();
               }
          } catch (Exception e) {
               try {
                    String unquoted = mapper.readValue(respBody, String.class);
                    return unquoted == null ? "" : unquoted;
               } catch (Exception ex) {
                    return respBody == null ? "" : respBody;
               }
          }

          // Safely extract text from possible response shapes
          String text = null;
          if (root.has("response") && !root.get("response").isNull()) {
               text = root.get("response").asText();
          } else if (root.has("results") && root.get("results").isArray() && root.get("results").size() > 0) {
               JsonNode first = root.get("results").get(0);
               if (first.has("content") && !first.get("content").isNull())
                    text = first.get("content").asText();
               else if (first.has("output") && !first.get("output").isNull())
                    text = first.get("output").asText();
               else
                    text = first.toString();
          } else if (root.has("outputs") && root.get("outputs").isArray() && root.get("outputs").size() > 0) {
               JsonNode first = root.get("outputs").get(0);
               if (first.has("content") && !first.get("content").isNull())
                    text = first.get("content").asText();
               else
                    text = first.toString();
          } else {
               // fallback to the entire body
               text = respBody == null ? "" : respBody;
          }

          return text == null ? "" : text;
     }

}
