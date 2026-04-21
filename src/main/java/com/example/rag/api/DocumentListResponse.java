package com.example.rag.api;

import java.util.List;

public class DocumentListResponse {
    private List<DocumentInfo> documents;

    public List<DocumentInfo> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentInfo> documents) {
        this.documents = documents;
    }
}