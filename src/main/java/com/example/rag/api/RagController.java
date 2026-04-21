package com.example.rag.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) throws Exception {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().body(new QueryResponse("Question is required."));
        }

        String answer = ragService.ask(request.getQuestion());
        return ResponseEntity.ok(new QueryResponse(answer));
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingest() {
        try {
            String message = ragService.ingestAll();
            return ResponseEntity.ok(new IngestResponse(message));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new IngestResponse("Ingestion failed: " + e.getMessage()));
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<DocumentListResponse> getDocuments() {
        List<DocumentInfo> documents = ragService.listDocuments();
        DocumentListResponse response = new DocumentListResponse();
        response.setDocuments(documents);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/compare")
    public ResponseEntity<ComparisonResponse> compareDocuments(@RequestBody ComparisonRequest request) {
        try {
            if (request == null || request.getDocument1Id() == null || request.getDocument2Id() == null
                    || request.getDocument1Id().isBlank() || request.getDocument2Id().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ComparisonResponse("Both document IDs are required."));
            }

            String comparison = ragService.compareDocuments(request.getDocument1Id(), request.getDocument2Id());
            return ResponseEntity.ok(new ComparisonResponse(comparison));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ComparisonResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ComparisonResponse("Comparison failed: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<IngestResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(ragService.uploadDocument(file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new IngestResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new IngestResponse("Upload failed: " + e.getMessage()));
        }
    }
}
