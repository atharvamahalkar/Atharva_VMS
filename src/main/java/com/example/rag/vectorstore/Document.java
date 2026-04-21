package com.example.rag.vectorstore;

import java.util.Map;

public class Document {
     private String id;
     private String content;
     private float[] embedding;
     private Map<String, Object> metadata;

     public Document(String content, float[] embedding) {
          this.content = content;
          this.embedding = embedding;
     }

     public Document(String id, String content, float[] embedding) {
          this.id = id;
          this.content = content;
          this.embedding = embedding;
     }

     public Document(String id, String content, float[] embedding, Map<String, Object> metadata) {
          this.id = id;
          this.content = content;
          this.embedding = embedding;
          this.metadata = metadata;
     }

     // Getters and setters
     public String getId() { return id; }
     public void setId(String id) { this.id = id; }
     
     public String getContent() { return content; }
     public void setContent(String content) { this.content = content; }
     
     public float[] getEmbedding() { return embedding; }
     public void setEmbedding(float[] embedding) { this.embedding = embedding; }
     
     public Map<String, Object> getMetadata() { return metadata; }
     public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}