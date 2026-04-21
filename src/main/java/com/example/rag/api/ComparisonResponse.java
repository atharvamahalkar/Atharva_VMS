package com.example.rag.api;

public class ComparisonResponse {
    private String comparison;

    public ComparisonResponse() {
    }

    public ComparisonResponse(String comparison) {
        this.comparison = comparison;
    }

    public String getComparison() {
        return comparison;
    }

    public void setComparison(String comparison) {
        this.comparison = comparison;
    }
}