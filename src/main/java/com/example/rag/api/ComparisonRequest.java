package com.example.rag.api;

public class ComparisonRequest {
    private String document1Id;
    private String document2Id;

    public ComparisonRequest() {
    }

    public ComparisonRequest(String document1Id, String document2Id) {
        this.document1Id = document1Id;
        this.document2Id = document2Id;
    }

    public String getDocument1Id() {
        return document1Id;
    }

    public void setDocument1Id(String document1Id) {
        this.document1Id = document1Id;
    }

    public String getDocument2Id() {
        return document2Id;
    }

    public void setDocument2Id(String document2Id) {
        this.document2Id = document2Id;
    }
}
