package com.example.rag.vectorstore;

import com.example.rag.config.Config;
import io.pinecone.clients.Index;
import io.pinecone.clients.Pinecone;
import io.pinecone.unsigned_indices_model.QueryResponseWithUnsignedIndices;
import io.pinecone.unsigned_indices_model.ScoredVectorWithUnsignedIndices;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import com.example.rag.embedding.OllamaEmbeddingService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VectorStore {

     private final Pinecone pinecone;
     private final Index index;
     private final Map<String, String> idToContentCache = new ConcurrentHashMap<>();

     public VectorStore() {
          // Initialize Pinecone client
          pinecone = new Pinecone.Builder(Config.PINECONE_API_KEY).build();

          // Auto-create index on startup
          try {
               new IndexInitializer(pinecone).createIndexIfNotExists();
          } catch (Exception e) {
               throw new RuntimeException("Failed to initialize Pinecone index", e);
          }

          // Connect to index
          index = pinecone.getIndexConnection(Config.INDEX_NAME);
     }

     // Convert float[] to List<Float> for Pinecone API
     private List<Float> toFloatList(float[] vector) {
          List<Float> list = new ArrayList<>(vector.length);
          for (float v : vector) {
               list.add(v);
          }
          return list;
     }

     /**
      * Upsert all lines from a text file
      * 
      * @param filePath         Path to the text file
      * @param embeddingService Ollama embedding service to generate embeddings
      * @return Number of documents upserted
      */

     /**
      * Insert a single document with its embedding
      */
     public void upsert(String id, String content, float[] embedding) {
          upsert(id, content, embedding, null);
     }

     /**
      * Insert a single document with metadata.
      */

     public void upsert(String id, String content, float[] embedding, Map<String, Object> metadata) {
          List<Float> vector = toFloatList(embedding);
          Struct.Builder metaBuilder = Struct.newBuilder();
          metaBuilder.putFields("text", Value.newBuilder().setStringValue(content).build());
          metaBuilder.putFields("content", Value.newBuilder().setStringValue(content).build());
          if (metadata != null) {
               for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof String) {
                         metaBuilder.putFields(key, Value.newBuilder().setStringValue((String) value).build());
                    } else if (value instanceof Number) {
                         metaBuilder.putFields(key,
                                   Value.newBuilder().setNumberValue(((Number) value).doubleValue()).build());
                    } else if (value instanceof Boolean) {
                         metaBuilder.putFields(key, Value.newBuilder().setBoolValue((Boolean) value).build());
                    }
               }
          }

          Struct metadataStruct = metaBuilder.build();
          index.upsert(id, vector, null, null, metadataStruct, Config.PINECONE_NAMESPACE);
          idToContentCache.put(id, content);
          System.out.println("Upserted: " + id);
     }

     /**
      * Insert a single document with auto-generated ID
      */
     public String upsert(String content, float[] embedding) {
          String id = UUID.randomUUID().toString();
          upsert(id, content, embedding);
          return id;
     }

     /**
      * Search for similar documents with default topK = 3
      */
     public List<String> search(float[] vector) {
          return search(vector, 3);
     }

     /**
      * Search for similar documents
      */
     public List<String> search(float[] vector, int topK) {
          return search(vector, topK, Config.PINECONE_NAMESPACE, null);
     }

     /**
      * Search for similar documents with optional namespace and metadata filter.
      */
     public List<String> search(float[] vector, int topK, String namespace, Struct filter) {
          List<Float> queryVector = toFloatList(vector);
          int requestedTopK = Math.max(topK, Math.min(50, topK * 5));
          // Use pinecone.query("my-index", vector, topK) directly
          QueryResponseWithUnsignedIndices response = index.query(
                    requestedTopK, // topK (over-fetch to skip stale vectors without metadata)
                    queryVector, // query vector
                    null, // sparseVector
                    null, // sparseVectorValues
                    null, // id
                    namespace, // namespace
                    filter, // filter
                    true, // includeMetadata
                    true // includeValues
          );

          List<String> results = new ArrayList<>();
          if (response == null || response.getMatchesList() == null) {
               System.out.println("VectorStore.search: no matches returned (response null)");
               return results;
          }

          System.out.println("VectorStore.search: matches returned = " + response.getMatchesList().size());

          for (ScoredVectorWithUnsignedIndices match : response.getMatchesList()) {
               String id = match.getId();
               float score = match.getScore();
               Struct metadata = match.getMetadata();

               String content = null;
               if (metadata != null) {
                    var fields = metadata.getFieldsMap();
                    System.out.println("  Match id=" + id + " score=" + score + " metadataKeys=" + fields.keySet());
                    if (fields.containsKey("text")) {
                         content = fields.get("text").getStringValue();
                    } else if (fields.containsKey("content")) {
                         content = fields.get("content").getStringValue();
                    }
               } else {
                    System.out.println("  Match id=" + id + " has no metadata");
               }

               if (content != null && !content.isBlank()) {
                    results.add(content);
                    if (results.size() >= topK) {
                         break;
                    }
               } else {
                    String fallback = idToContentCache.get(id);
                    if (fallback != null && !fallback.isBlank()) {
                         results.add(fallback);
                         if (results.size() >= topK) {
                              break;
                         }
                    } else {
                         System.out.println("  Skipping empty content for id=" + id);
                    }
               }
          }

          return results;
     }

     /**
      * Search and return detailed results with scores
      */
     public List<SearchResult> searchWithScores(float[] vector, int topK) {
          return searchWithScores(vector, topK, Config.PINECONE_NAMESPACE, null);
     }

     /**
      * Search and return detailed results with scores, with optional namespace and
      * metadata filter.
      */
     public List<SearchResult> searchWithScores(float[] vector, int topK, String namespace, Struct filter) {
          List<Float> queryVector = toFloatList(vector);
          int requestedTopK = Math.max(topK, Math.min(50, topK * 5));
          // Use pinecone.query("my-index", vector, topK) directly
          QueryResponseWithUnsignedIndices response = index.query(
                    requestedTopK, // topK (over-fetch to skip stale vectors without metadata)
                    queryVector, // query vector
                    null, // sparseVector
                    null, // sparseVectorValues
                    null, // id
                    namespace, // namespace
                    filter, // filter
                    true, // includeMetadata
                    false // includeValues
          );

          List<SearchResult> results = new ArrayList<>();
          if (response == null || response.getMatchesList() == null) {
               System.out.println("VectorStore.searchWithScores: no matches returned (response null)");
               return results;
          }
          for (ScoredVectorWithUnsignedIndices match : response.getMatchesList()) {
               String id = match.getId();
               String content = null;
               Map<String, Object> metadataMap = new HashMap<>();

               Struct metadata = match.getMetadata();
               System.out.println("  🔍 Search result id=" + id + " score=" + match.getScore()
                         + " hasMetadata=" + (metadata != null) + " numFields="
                         + (metadata != null ? metadata.getFieldsMap().size() : 0));

               if (metadata != null) {
                    System.out.println("    Metadata keys: " + metadata.getFieldsMap().keySet());
                    // Convert Struct to Map
                    for (Map.Entry<String, Value> entry : metadata.getFieldsMap().entrySet()) {
                         Value value = entry.getValue();
                         System.out.println("    Field '" + entry.getKey() + "' = " + value.getStringValue());
                         switch (value.getKindCase()) {
                              case STRING_VALUE:
                                   metadataMap.put(entry.getKey(), value.getStringValue());
                                   if ("text".equals(entry.getKey())) {
                                        content = value.getStringValue();
                                   } else if ("content".equals(entry.getKey()) && content == null) {
                                        content = value.getStringValue();
                                   }
                                   break;
                              case NUMBER_VALUE:
                                   metadataMap.put(entry.getKey(), value.getNumberValue());
                                   break;
                              case BOOL_VALUE:
                                   metadataMap.put(entry.getKey(), value.getBoolValue());
                                   break;
                              default:
                                   metadataMap.put(entry.getKey(), value.toString());
                         }
                    }
               } else {
                    System.out.println("    ⚠️  No metadata returned for this match");
               }

               float score = match.getScore();
               if (content != null && !content.isBlank()) {
                    results.add(new SearchResult(id, content, score, metadataMap));
                    if (results.size() >= topK) {
                         break;
                    }
               } else {
                    String fallback = idToContentCache.get(id);
                    if (fallback != null && !fallback.isBlank()) {
                         metadataMap.put("content_source", "local_cache_fallback");
                         results.add(new SearchResult(id, fallback, score, metadataMap));
                         if (results.size() >= topK) {
                              break;
                         }
                    } else {
                         System.out.println("    Skipping result without content for id=" + id);
                    }
               }
          }

          return results;
     }

     /**
      * Delete a document by ID
      */
     public void delete(String id) {
          index.delete(Collections.singletonList(id), false, Config.PINECONE_NAMESPACE, null);
          idToContentCache.remove(id);
          System.out.println("✅ Deleted: " + id);
     }

     /**
      * Read a plaintext knowledge file, split into paragraphs (blank-line
      * separated),
      * embed each paragraph using the provided embedding service, and upsert into
      * the index.
      * This uses deterministic IDs (SHA-1 of the content) so repeated uploads are
      * idempotent.
      */
     public void deleteBySource(String sourceFileName) {
          if (sourceFileName == null || sourceFileName.isBlank()) {
               return;
          }

          Struct.Builder equalsBuilder = Struct.newBuilder();
          equalsBuilder.putFields("$eq", Value.newBuilder().setStringValue(sourceFileName).build());

          Struct.Builder filterBuilder = Struct.newBuilder();
          filterBuilder.putFields("source", Value.newBuilder().setStructValue(equalsBuilder.build()).build());

          index.delete(null, false, Config.PINECONE_NAMESPACE, filterBuilder.build());
          System.out.println("Deleted existing vectors for source: " + sourceFileName);
     }

     public void upsertFromFile(String filePath, OllamaEmbeddingService embeddingService) {
          try {
               Path path = Path.of(filePath);
               if (!Files.exists(path)) {
                    System.out.println("Knowledge file not found: " + filePath);
                    return;
               }

               String text = Files.readString(path, StandardCharsets.UTF_8);
               List<String> passages = Arrays.stream(text.split("\\r?\\n\\r?\\n"))
                         .map(String::trim)
                         .filter(s -> !s.isEmpty())
                         .toList();

               if (passages.size() <= 1) {
                    passages = Arrays.stream(text.split("\\r?\\n"))
                              .map(String::trim)
                              .filter(s -> !s.isEmpty())
                              .toList();
               }

               System.out.println("Loaded " + passages.size() + " passages from file");

               List<Document> docs = new ArrayList<>();
               for (int i = 0; i < passages.size(); i++) {
                    try {
                         String passage = passages.get(i);
                         System.out.println("Embedding passage " + (i + 1) + "/" + passages.size() + "...");
                         float[] emb = embeddingService.generateEmbedding(passage);
                         byte[] sha = MessageDigest.getInstance("SHA-1")
                                   .digest(passage.getBytes(StandardCharsets.UTF_8));
                         StringBuilder sb = new StringBuilder();
                         for (byte b : sha)
                              sb.append(String.format("%02x", b));
                         String id = "kb-" + sb.substring(0, 12);

                         docs.add(new Document(id, passage, emb,
                                   Map.of("source", "knowledge2.txt", "chunk_index", i, "genre", "knowledge")));
                    } catch (Exception e) {
                         System.out.println("⚠️  Failed to embed passage " + (i + 1) + ": " + e.getMessage());
                         e.printStackTrace();
                    }
               }

               if (!docs.isEmpty()) {
                    System.out.println("Prepared " + docs.size() + " knowledge passages. Sample ids:");
                    docs.stream().limit(5).forEach(
                              d -> System.out.println("  - " + d.getId() + " (len=" + d.getContent().length() + ")"));
                    batchUpsert(docs);
                    System.out.println("✅ Uploaded " + docs.size() + " knowledge passages to index");
               } else {
                    System.out.println("⚠️  No documents to upload after processing");
               }
          } catch (Exception e) {
               System.out.println(
                         "❌ Failed to upload knowledge file: " + e.getClass().getSimpleName() + " - " + e.getMessage());
               e.printStackTrace();
               throw new RuntimeException(
                         "Failed to upload knowledge file: " + e.getClass().getSimpleName() + " - " + e.getMessage(),
                         e);
          }
     }

     /**
      * Get index statistics
      */

     /**
      * Batch upsert documents into the index. Uses the official Pinecone API pattern
      * with the builder to ensure metadata is properly persisted.
      */
     public void batchUpsert(List<Document> documents) {
          if (documents == null || documents.isEmpty()) {
               return;
          }

          int successCount = 0;
          for (Document doc : documents) {
               if (doc == null || doc.getContent() == null || doc.getEmbedding() == null) {
                    System.out.println("Skipping null or incomplete document");
                    continue;
               }

               String id = doc.getId() != null ? doc.getId() : UUID.randomUUID().toString();
               List<Float> vector = toFloatList(doc.getEmbedding());

               Struct.Builder metaBuilder = Struct.newBuilder();
               metaBuilder.putFields("text", Value.newBuilder().setStringValue(doc.getContent()).build());
               metaBuilder.putFields("content", Value.newBuilder().setStringValue(doc.getContent()).build());
               if (doc.getMetadata() != null) {
                    for (Map.Entry<String, Object> entry : doc.getMetadata().entrySet()) {
                         String key = entry.getKey();
                         Object value = entry.getValue();
                         if (value instanceof String) {
                              metaBuilder.putFields(key, Value.newBuilder().setStringValue((String) value).build());
                         } else if (value instanceof Number) {
                              metaBuilder.putFields(key,
                                        Value.newBuilder().setNumberValue(((Number) value).doubleValue()).build());
                         } else if (value instanceof Boolean) {
                              metaBuilder.putFields(key, Value.newBuilder().setBoolValue((Boolean) value).build());
                         }
                    }
               }
               Struct metadataStruct = metaBuilder.build();

               try {
                    index.upsert(id, vector, null, null, metadataStruct, Config.PINECONE_NAMESPACE);
                    idToContentCache.put(id, doc.getContent());
                    successCount++;
               } catch (Exception e) {
                    System.out.println("Failed to upsert vector for id=" + id + ": " + e.getMessage());
                    e.printStackTrace();
               }
          }

          if (successCount == 0) {
               throw new RuntimeException("Failed to upsert any documents to Pinecone.");
          }
          System.out.println("Batch upsert completed. Successful documents: " + successCount);
     }

     public void printStats() {
          var stats = index.describeIndexStats();
          System.out.println("📊 Index Statistics:");
          System.out.println("   Total vectors: " + stats.getTotalVectorCount());
          System.out.println("   Dimension: " + stats.getDimension());
     }

     /**
      * Delete all vectors from the index (useful if index contains old vectors
      * without metadata).
      * WARNING: This clears the entire index.
      */
     public void deleteAll() {
          try {
               System.out.println("Deleting all vectors from index...");
               index.delete(null, true, Config.PINECONE_NAMESPACE, null);
               idToContentCache.clear();
               System.out.println("✅ All vectors deleted. Index is now empty.");
          } catch (Exception e) {
               System.out.println("⚠️  Delete all may not be supported; trying alternative approach...");
               throw new RuntimeException("Failed to delete all vectors: " + e.getMessage(), e);
          }
     }

     /**
      * Recreate the index from scratch and reload knowledge.
      */
     public void recreateIndexAndReloadKnowledge(String knowledgeFilePath, OllamaEmbeddingService embeddingService) {
          try {
               System.out.println("🔄 Starting index recreation...");
               System.out.println("   1. Deleting old index '" + Config.INDEX_NAME + "'...");

               try {
                    pinecone.deleteIndex(Config.INDEX_NAME);
                    System.out.println("   ✓ Index deleted successfully");
               } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("not found")) {
                         System.out.println("   ℹ️  Index didn't exist (OK)");
                    } else {
                         System.out.println("   ⚠️  Error deleting index: " + e.getMessage());
                    }
               }

               System.out.println("   2. Waiting 2 seconds for cleanup...");
               Thread.sleep(2000);
               System.out.println("   ✓ Wait complete");

               System.out.println("   3. Creating new index...");
               new IndexInitializer(pinecone).createIndexIfNotExists();
               System.out.println("   ✓ New index created");

               if (knowledgeFilePath != null && !knowledgeFilePath.isBlank()) {
                    System.out.println("   4. Uploading knowledge from: " + knowledgeFilePath);
                    upsertFromFile(knowledgeFilePath, embeddingService);
                    System.out.println("✅ Index recreation complete!");
               } else {
                    System.out.println("⚠️  No knowledge file path provided");
               }
          } catch (Exception e) {
               System.out.println(
                         "❌ Failed to recreate index: " + e.getClass().getSimpleName() + " - " + e.getMessage());
               e.printStackTrace();
               throw new RuntimeException(
                         "Failed to recreate index: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
          }
     }
}
