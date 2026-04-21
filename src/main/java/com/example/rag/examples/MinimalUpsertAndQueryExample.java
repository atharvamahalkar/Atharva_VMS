package com.example.rag.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rag.config.Config;

import io.pinecone.clients.Index;
import io.pinecone.clients.Pinecone;
import io.pinecone.proto.UpsertResponse;
import io.pinecone.unsigned_indices_model.QueryResponseWithUnsignedIndices;

import java.util.ArrayList;
import java.util.List;

public class MinimalUpsertAndQueryExample {
     private static final Logger logger = LoggerFactory.getLogger(MinimalUpsertAndQueryExample.class);

     public static class Args {
          public String apiKey = System.getProperty("pinecone.apikey", Config.PINECONE_API_KEY);
          String indexName = System.getProperty("pinecone.indexName", Config.INDEX_NAME);
          String namespace = "test-ns";
          int topK = 1;
     }

     public static void main(String[] cliArgs) {
          System.out.println("Starting application...");

          Args args = new Args();
          Pinecone pinecone = new Pinecone.Builder(args.apiKey).build();

          try {
               Index index = pinecone.getIndexConnection(args.indexName);
               logger.info("Sending upsert request.");

               List<Float> vector = new ArrayList<>();
               for (int i = 0; i < 768; i++) {
                    vector.add(1.0f);
               }

               UpsertResponse upsertResponse = index.upsert("v1", vector, args.namespace);
               logger.info("Got upsert response: " + upsertResponse);

               logger.info("Sending query request");
               QueryResponseWithUnsignedIndices queryResponse =
                         index.queryByVectorId(args.topK, "v1", args.namespace, true, false);

               logger.info("Got query response: " + queryResponse);
          } catch (Exception e) {
               e.printStackTrace();
          }
     }
}