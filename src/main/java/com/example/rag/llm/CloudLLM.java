package com.example.rag.llm;

public class CloudLLM implements LLMProvider {

     @Override
     public String generate(String prompt) {
          return "Cloud LLM response (Gemini/OpenAI):\n" + prompt;
     }
}