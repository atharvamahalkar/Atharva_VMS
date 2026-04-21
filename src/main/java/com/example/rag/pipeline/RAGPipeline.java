package com.example.rag.pipeline;

import java.util.ArrayList;
import java.util.List;

import com.example.rag.config.Config;
import com.example.rag.embedding.EmbeddingProvider;
import com.example.rag.embedding.OllamaEmbedding;
import com.example.rag.embedding.OpenAIEmbedding;
import com.example.rag.llm.CloudLLM;
import com.example.rag.llm.LLMProvider;
import com.example.rag.llm.OllamaLLM;
import com.example.rag.vectorstore.SearchResult;
import com.example.rag.vectorstore.VectorStore;

public class RAGPipeline {

     private final EmbeddingProvider embeddingProvider;
     private final LLMProvider llmProvider;
     private final VectorStore vectorStore;

     public RAGPipeline() {
          this(new VectorStore());
     }

     public RAGPipeline(VectorStore vectorStore) {
          this.vectorStore = vectorStore;

          boolean useOllama = Config.OLLAMA_MODEL != null &&
                    !Config.OLLAMA_MODEL.isBlank();

          embeddingProvider = useOllama
                    ? new OllamaEmbedding()
                    : new OpenAIEmbedding();

          llmProvider = useOllama
                    ? new OllamaLLM()
                    : new CloudLLM();
     }

     public String ask(String question) throws Exception {

          float[] queryEmbedding = embeddingProvider.embed(question);
          List<SearchResult> rawResults = vectorStore.searchWithScores(queryEmbedding, 5);

          if (rawResults == null || rawResults.isEmpty()) {
               return "I don't know based on the provided documents.";
          }

          List<SearchResult> validResults = new ArrayList<>();
          StringBuilder contextBuilder = new StringBuilder();

          for (SearchResult result : rawResults) {
               if (result == null) continue;

               String text = result.getText();
               if (text == null || text.isBlank()) continue;

               validResults.add(result);
               contextBuilder.append("Document Chunk:\n");
               contextBuilder.append(text.trim()).append("\n\n");
          }

          String context = contextBuilder.toString().trim();

          if (context.isEmpty()) {
               return "I don't know based on the provided documents.";
          }

          String prompt = buildPrompt(question, context);

          String answer = llmProvider.generate(prompt);

          if (answer == null || answer.isBlank()) {
               return "I don't know based on the provided documents.";
          }

          return answer.trim();
     }

     private String buildPrompt(String question, String context) {
          return """
You are a document question-answering assistant.

Answer the user's question using ONLY the context provided below.
If the answer is explicitly present in the context, answer clearly and directly.
Do NOT say "I don't know" if the answer is present in the context.
If the answer is not present in the context, reply exactly:
I don't know based on the provided documents.

Context:
%s

Question:
%s

Answer:
""".formatted(context, question);
     }
}