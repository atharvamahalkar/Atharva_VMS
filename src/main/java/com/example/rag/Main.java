package com.example.rag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

import com.example.rag.embedding.OllamaEmbeddingService;
import com.example.rag.ingestion.DocumentIngestionService;
import com.example.rag.pipeline.RAGPipeline;
import com.example.rag.vectorstore.VectorStore;

public class Main {
     private static final String KNOWLEDGE_FILE = "knowledge2.txt";

     private static final String MINIMAL_COMMAND = "minimal";
     private static final String INGEST_COMMAND = "ingest";
     private static final String EXIT_COMMAND = "exit";

     public static void main(String[] args) throws Exception {
          OllamaEmbeddingService embeddingService = new OllamaEmbeddingService();
          if (!isOllamaAvailable(embeddingService)) {
               return;
          }

          VectorStore vectorStore = new VectorStore();
          RAGPipeline ragPipeline = new RAGPipeline(vectorStore);

          // Keep this disabled while testing your own documents
          // loadKnowledge(vectorStore, embeddingService, hasFlag(args,
          // RECREATE_INDEX_FLAG));

          try (Scanner scanner = new Scanner(System.in)) {
               runInteractiveLoop(scanner, ragPipeline, vectorStore, embeddingService);
          }

          System.out.println();
          vectorStore.printStats();
     }

     private static boolean isOllamaAvailable(OllamaEmbeddingService embeddingService) throws Exception {
          if (!embeddingService.isAvailable()) {
               System.err.println("Ollama is not running. Please start it first.");
               return false;
          }

          System.out.println("Ollama is running");
          System.out.println("Available models: " + embeddingService.listModels());
          return true;
     }

     private static String findKnowledgeFile() {
          String[] candidatePaths = {
                    "src/main/resources/" + KNOWLEDGE_FILE,
                    "src/main/java/com/example/rag/" + KNOWLEDGE_FILE,
                    KNOWLEDGE_FILE
          };

          for (String candidatePath : candidatePaths) {
               if (Files.exists(Path.of(candidatePath))) {
                    return candidatePath;
               }
          }

          return copyKnowledgeFileFromClasspath();
     }

     private static String copyKnowledgeFileFromClasspath() {
          var resource = Main.class.getClassLoader().getResourceAsStream(KNOWLEDGE_FILE);
          if (resource == null) {
               return null;
          }

          Path outputPath = Path.of(KNOWLEDGE_FILE);
          try (var inputStream = resource) {
               Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
               return outputPath.toString();
          } catch (Exception e) {
               System.err.println("Failed to copy knowledge2.txt from classpath: " + e.getMessage());
               return null;
          }
     }

     private static void runInteractiveLoop(
               Scanner scanner,
               RAGPipeline ragPipeline,
               VectorStore vectorStore,
               OllamaEmbeddingService embeddingService) throws Exception {

          printInstructions();

          while (true) {
               System.out.print("\nUser: ");
               String userInput = scanner.nextLine();

               if (shouldExit(userInput)) {
                    System.out.println("Exiting realtime prompt system.");
                    return;
               }

               String command = userInput.trim().toLowerCase();

               if (MINIMAL_COMMAND.equals(command)) {
                    com.example.rag.examples.MinimalUpsertAndQueryExample.main(new String[0]);
                    continue;
               }

               if (INGEST_COMMAND.equals(command)) {
                    runIngestion(vectorStore, embeddingService);
                    continue;
               }

               answerQuestion(userInput, ragPipeline, vectorStore, embeddingService);
          }
     }

     private static void printInstructions() {
          System.out.println("\n--- Realtime RAG System ---");
          System.out.println("Type your question and press Enter (type 'exit' to quit)");
          System.out.println("Type 'minimal' to run MinimalUpsertAndQueryExample");
          System.out.println(
                    "Type 'ingest' to upload documents from data/contracts, data/invoices, data/policies, data/logs");
     }

     private static boolean shouldExit(String userInput) {
          return userInput == null || EXIT_COMMAND.equalsIgnoreCase(userInput.trim());
     }

     private static void runIngestion(
               VectorStore vectorStore,
               OllamaEmbeddingService embeddingService) {
          try {
               System.out.println("\nStarting document ingestion...");

               ingestIfExists("data/contracts", vectorStore, embeddingService);
               ingestIfExists("data/invoices", vectorStore, embeddingService);
               ingestIfExists("data/policies", vectorStore, embeddingService);
               ingestIfExists("data/logs", vectorStore, embeddingService);

               System.out.println("Document ingestion completed.");
          } catch (Exception e) {
               System.err.println("Ingestion failed: " + e.getMessage());
               e.printStackTrace();
          }
     }

     private static void ingestIfExists(
               String folderPath,
               VectorStore vectorStore,
               OllamaEmbeddingService embeddingService) throws Exception {
          Path path = Path.of(folderPath);
          if (Files.exists(path) && Files.isDirectory(path)) {
               System.out.println("Ingesting folder: " + folderPath);
               DocumentIngestionService.ingestFolder(folderPath, vectorStore, embeddingService);
          } else {
               System.out.println("Skipping missing folder: " + folderPath);
          }
     }

     private static void answerQuestion(
               String userInput,
               RAGPipeline ragPipeline,
               VectorStore vectorStore,
               OllamaEmbeddingService embeddingService) throws Exception {

          String answer = ragPipeline.ask(userInput);
          System.out.println("\nAnswer:\n" + answer);

          float[] queryEmbedding = embeddingService.generateEmbedding(userInput);
          var scored = vectorStore.searchWithScores(queryEmbedding, 3);

          System.out.println("\nTop Retrieved Context:");
          if (scored.isEmpty()) {
               System.out.println("  (no retrieved context)");
               return;
          }

          for (var result : scored) {
               System.out.println("  - id=" + result.getId() + " score=" + result.getScore());
               System.out.println("    text='" + result.getText() + "'");
               System.out.println("    metadataKeys="
                         + (result.getMetadata() == null ? "[]" : result.getMetadata().keySet()));
          }
     }
}