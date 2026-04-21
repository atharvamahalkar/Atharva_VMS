package com.example.rag.util;

import com.example.rag.vectorstore.SearchResult;
import java.util.List;

public class PromptBuilder {

     public static String build(String question, List<SearchResult> results) {
          StringBuilder ctx = new StringBuilder();
          for (SearchResult r : results) {
               String text = r.getText() == null ? "" : r.getText();
               ctx.append("- [").append(r.getId()).append("] ").append(text.replaceAll("\n", " ")).append("\n");
          }

          return """
                    You are an expert assistant.
                    You MUST answer ONLY and ONLY using the context given below. Do NOT Perform any calculations. Do NOT use any outside knowledge or make assumptions.
                    If the answer cannot be found in the provided context, reply exactly: "I don't know based on the provided documents." and nothing else.

                    Context:
                    %s

                    Question:
                    %s
                    """
                    .formatted(ctx.toString().trim(), question);
     }

     // Backwards-compatible builder (for simple List<String> contexts)
     public static String buildFromStrings(String question, List<String> context) {
          // Convert plain strings to pseudo SearchResult entries
          StringBuilder ctx = new StringBuilder();
          int i = 1;
          for (String s : context) {
               ctx.append("- [p").append(i++).append("] ").append(s.replaceAll("\n", " ")).append("\n");
          }
          return """
                    You are an expert assistant.
                    You MUST answer ONLY using the context below. Do NOT use any outside knowledge or make assumptions.
                    If the answer cannot be found in the provided context, reply exactly: "I don't know based on the provided documents." and nothing else.

                    Context:
                    %s

                    Question:
                    %s
                    """
                    .formatted(ctx.toString().trim(), question);
     }
}