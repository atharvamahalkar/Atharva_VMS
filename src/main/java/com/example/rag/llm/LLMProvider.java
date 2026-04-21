package com.example.rag.llm;

public interface LLMProvider {
     String generate(String prompt) throws Exception;
}
