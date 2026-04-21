package com.example.rag.vectorstore;

import java.util.Map;

public class SearchResult {
    private final String id;
    private final String text;
    private final float score;
    private final Map<String, Object> metadata;

    public SearchResult(String id, String text, float score, Map<String, Object> metadata) {
        this.id = id;
        this.metadata = metadata;
        this.text = text;
        this.score = score;

    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public float getScore() {
        return score;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return String.format("SearchResult{id='%s', score=%.4f, text='%s'}", id, score, text);
    }
}
