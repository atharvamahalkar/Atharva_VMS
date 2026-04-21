package com.example.rag.config;

public class Config {

    public static final String OLLAMA_BASE_URL =
        System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");

    public static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");

    public static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");

    public static final String ELASTIC_URL =
        System.getenv().getOrDefault("ELASTIC_URL", "http://localhost:9200");

    public static final String PINECONE_API_KEY = System.getenv("PINECONE_API_KEY");

    public static final String OLLAMA_EMBEDDING_MODEL =
        System.getenv().getOrDefault("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text");

    public static final String OLLAMA_MODEL = resolveOllamaGenerationModel();

    public static final int EMBEDDING_DIM =
        Integer.parseInt(System.getenv().getOrDefault("EMBEDDING_DIM", "768"));

    public static final String INDEX_NAME =
        System.getenv().getOrDefault("PINECONE_INDEX_NAME", "vendor-rag-clean");

    public static final String PINECONE_NAMESPACE =
        System.getenv().getOrDefault("PINECONE_NAMESPACE", "vendor-docs");

    public static final String PINECONE_CLOUD =
        System.getenv().getOrDefault("PINECONE_CLOUD", "aws");

    public static final String PINECONE_REGION =
        System.getenv().getOrDefault("PINECONE_REGION", "us-east-1");

    static {
        if (PINECONE_API_KEY == null || PINECONE_API_KEY.isEmpty()) {
            throw new RuntimeException("PINECONE_API_KEY is not set");
        }
    }

    private static String resolveOllamaGenerationModel() {
        String explicitGenerationModel = System.getenv("OLLAMA_GENERATION_MODEL");
        if (explicitGenerationModel != null && !explicitGenerationModel.isBlank()) {
            return explicitGenerationModel;
        }

        String configuredModel = System.getenv().getOrDefault("OLLAMA_MODEL", "qwen:0.5b");
        if (configuredModel.equalsIgnoreCase(OLLAMA_EMBEDDING_MODEL)) {
            return "qwen:0.5b";
        }

        return configuredModel;
    }
}