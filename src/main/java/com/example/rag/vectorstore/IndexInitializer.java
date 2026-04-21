package com.example.rag.vectorstore;

import com.example.rag.config.Config;

import io.pinecone.clients.Pinecone;
import org.openapitools.db_control.client.model.DeletionProtection;
import org.openapitools.db_control.client.model.IndexList;
import org.openapitools.db_control.client.model.IndexModel;

public class IndexInitializer {

     private final Pinecone pinecone;

     public IndexInitializer(Pinecone pinecone) {
          this.pinecone = pinecone;
     }

     public void createIndexIfNotExists() throws Exception {
          boolean exists = indexExists(Config.INDEX_NAME);

          if (exists) {
               IndexModel model = pinecone.describeIndex(Config.INDEX_NAME);
               if (model != null && model.getDimension() != null && model.getDimension() != Config.EMBEDDING_DIM) {
                    throw new RuntimeException(
                              "Pinecone index '" + Config.INDEX_NAME + "' dimension (" + model.getDimension()
                                        + ") does not match expected embedding dimension (" + Config.EMBEDDING_DIM
                                        + "). Please recreate the index with the correct dimension.");
               }

               System.out.println("Pinecone index already exists: " + Config.INDEX_NAME);
               return;
          }

          System.out.println("Creating Pinecone index: " + Config.INDEX_NAME);
          System.out.println("Cloud: " + Config.PINECONE_CLOUD + ", Region: " + Config.PINECONE_REGION);

          try {
               pinecone.createServerlessIndex(
                         Config.INDEX_NAME,
                         "cosine",
                         Config.EMBEDDING_DIM,
                         Config.PINECONE_CLOUD,
                         Config.PINECONE_REGION,
                         DeletionProtection.DISABLED);
          } catch (Exception e) {
               throw new RuntimeException(
                         "Failed to create Pinecone index '" + Config.INDEX_NAME + "' in "
                                   + Config.PINECONE_CLOUD + "/" + Config.PINECONE_REGION + ": "
                                   + safeMessage(e),
                         e);
          }

          waitForIndexReady(Config.INDEX_NAME);
          System.out.println("Pinecone index created successfully");
     }

     private boolean indexExists(String indexName) throws Exception {
          try {
               IndexList indexList = pinecone.listIndexes();
               if (indexList.getIndexes() != null) {
                    return indexList.getIndexes().stream()
                              .anyMatch(index -> indexName.equals(index.getName()));
               }
               return false;
          } catch (Exception e) {
               throw new RuntimeException(
                         "Failed to list Pinecone indexes for '" + indexName + "': " + safeMessage(e),
                         e);
          }
     }

     private void waitForIndexReady(String indexName) throws InterruptedException {
          System.out.println("Waiting for Pinecone index to become ready...");

          int maxWaitSeconds = 120;
          int waited = 0;

          while (waited < maxWaitSeconds) {
               try {
                    IndexModel indexModel = pinecone.describeIndex(indexName);

                    if (indexModel.getStatus() != null &&
                              indexModel.getStatus().getReady() != null &&
                              indexModel.getStatus().getReady()) {
                         System.out.println("Index is ready");
                         return;
                    }
               } catch (Exception e) {
                    System.out.println("Still waiting for index readiness: " + safeMessage(e));
               }

               Thread.sleep(3000);
               waited += 3;
          }

          throw new RuntimeException("Index creation timed out after " + maxWaitSeconds + " seconds");
     }

     private String safeMessage(Exception e) {
          return e.getMessage() == null || e.getMessage().isBlank()
                    ? e.getClass().getSimpleName()
                    : e.getMessage();
     }
}
