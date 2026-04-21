package com.example.rag.api;

public class IngestResponse {
    private String message;
    private String documentId;
    private String fileName;

    public IngestResponse() {
    }

    public IngestResponse(String message) {
        this.message = message;
    }

    public IngestResponse(String message, String documentId, String fileName) {
        this.message = message;
        this.documentId = documentId;
        this.fileName = fileName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
