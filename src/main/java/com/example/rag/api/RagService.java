package com.example.rag.api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.rag.config.Config;
import com.example.rag.embedding.OllamaEmbeddingService;
import com.example.rag.ingestion.DocumentIngestionService;
import com.example.rag.llm.CloudLLM;
import com.example.rag.llm.LLMProvider;
import com.example.rag.llm.OllamaLLM;
import com.example.rag.pipeline.RAGPipeline;
import com.example.rag.vectorstore.SearchResult;
import com.example.rag.vectorstore.VectorStore;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

@Service
public class RagService {

    private static final Path DATA_ROOT = Paths.get("data").toAbsolutePath().normalize();
    private static final List<String> DOCUMENT_FOLDERS = List.of(
            "contracts",
            "invoices",
            "policies",
            "logs",
            "uploads");
    private static final int MAX_COMPARE_CHARS_PER_DOCUMENT = 12000;

    private VectorStore vectorStore;
    private RAGPipeline ragPipeline;
    private OllamaEmbeddingService embeddingService;
    private LLMProvider comparisonLLM;

    public RagService() {
    }

    private void initializeServices() throws Exception {
        if (vectorStore == null) {
            this.vectorStore = new VectorStore();
            this.ragPipeline = new RAGPipeline(vectorStore);
            this.embeddingService = new OllamaEmbeddingService();
            this.comparisonLLM = isBlank(Config.OLLAMA_MODEL) ? new CloudLLM() : new OllamaLLM();
        }
    }

    public String ask(String question) throws Exception {
        initializeServices();
        return ragPipeline.ask(question);
    }

    public String ingestAll() throws Exception {
        initializeServices();
        StringBuilder result = new StringBuilder();

        for (String folder : DOCUMENT_FOLDERS) {
            if ("uploads".equals(folder)) {
                continue;
            }
            result.append(ingestFolder(resolveManagedFolder(folder).toString()));
            result.append("\n");
        }

        return result.toString().trim();
    }

    public List<DocumentInfo> listDocuments() {
        List<DocumentInfo> documents = new ArrayList<>();

        for (String folder : DOCUMENT_FOLDERS) {
            Path directory = resolveManagedFolder(folder);
            if (!Files.isDirectory(directory)) {
                continue;
            }

            try (Stream<Path> stream = Files.list(directory)) {
                stream.filter(Files::isRegularFile)
                        .filter(this::isSupportedDocument)
                        .sorted(Comparator.comparing(this::safeLastModified).reversed())
                        .map(path -> toDocumentInfo(folder, path))
                        .forEach(documents::add);
            } catch (IOException e) {
                // Skip unreadable directories without failing the whole endpoint.
            }
        }

        return documents;
    }

    public String compareDocuments(String document1Id, String document2Id) throws Exception {
        initializeServices();

        ManagedDocument document1 = getManagedDocument(document1Id);
        ManagedDocument document2 = getManagedDocument(document2Id);

        String comparisonQuery =
                "Compare payment terms, delivery terms, penalties, obligations, risks, and recommendations.";
        float[] comparisonEmbedding = embeddingService.generateEmbedding(comparisonQuery);

        List<SearchResult> document1Chunks = vectorStore.searchWithScores(
                comparisonEmbedding,
                4,
                Config.PINECONE_NAMESPACE,
                buildSourceFilter(document1.fileName()));
        List<SearchResult> document2Chunks = vectorStore.searchWithScores(
                comparisonEmbedding,
                4,
                Config.PINECONE_NAMESPACE,
                buildSourceFilter(document2.fileName()));

        if (document1Chunks.isEmpty() || document2Chunks.isEmpty()) {
            return """
=== SUMMARY ===

I could not compare these documents from the indexed RAG context alone.

=== RECOMMENDATIONS ===

- Ingest the selected documents first.
- Confirm the selected files are present in the vector index.
- Retry the comparison after ingestion completes.
""";
        }

        String prompt = buildRagComparisonPrompt(document1, document1Chunks, document2, document2Chunks);
        String comparison = comparisonLLM.generate(prompt);
        return comparison == null || comparison.isBlank()
                ? "I don't know based on the provided documents."
                : comparison.trim();
    }

    public IngestResponse uploadDocument(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalName = sanitizeFileName(file.getOriginalFilename());
        if (!originalName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            throw new IllegalArgumentException("Only .txt uploads are supported");
        }

        Path uploadDir = resolveManagedFolder("uploads");
        Files.createDirectories(uploadDir);

        String storedName = System.currentTimeMillis() + "_" + originalName;
        Path target = uploadDir.resolve(storedName).normalize();
        ensureUnderRoot(target, uploadDir);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String documentId = buildDocumentId("uploads", storedName);
        return new IngestResponse("File uploaded successfully", documentId, storedName);
    }

    private ManagedDocument getManagedDocument(String documentId) {
        if (isBlank(documentId)) {
            throw new IllegalArgumentException("Document ID is required");
        }

        String normalizedId = documentId.replace('\\', '/');
        int slashIndex = normalizedId.indexOf('/');
        if (slashIndex <= 0 || slashIndex == normalizedId.length() - 1) {
            throw new IllegalArgumentException("Invalid document ID");
        }

        String folder = normalizedId.substring(0, slashIndex);
        String fileName = normalizedId.substring(slashIndex + 1);
        Path directory = resolveManagedFolder(folder);
        Path filePath = directory.resolve(fileName).normalize();
        ensureUnderRoot(filePath, directory);

        if (!Files.isRegularFile(filePath) || !isSupportedDocument(filePath)) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        return new ManagedDocument(
                documentId,
                folder,
                filePath.getFileName().toString(),
                getDocType(filePath.getFileName().toString()));
    }

    private String buildRagComparisonPrompt(
            ManagedDocument document1,
            List<SearchResult> document1Chunks,
            ManagedDocument document2,
            List<SearchResult> document2Chunks) {
        return """
You are a contract analysis assistant.

Compare the two documents below using only the retrieved RAG context.
Be specific and grounded in the retrieved text. Do not invent clauses.
If the retrieved context is insufficient, say so plainly.

Return sections with these exact headings:
=== SUMMARY ===
=== KEY SIMILARITIES ===
=== KEY DIFFERENCES ===
=== RISK ASSESSMENT ===
=== RECOMMENDATIONS ===

DOCUMENT 1
ID: %s
Type: %s
Name: %s
Retrieved Context:
%s

DOCUMENT 2
ID: %s
Type: %s
Name: %s
Retrieved Context:
%s
""".formatted(
                document1.documentId(),
                document1.documentType(),
                document1.fileName(),
                formatRetrievedContext(document1Chunks),
                document2.documentId(),
                document2.documentType(),
                document2.fileName(),
                formatRetrievedContext(document2Chunks));
    }

    private Struct buildSourceFilter(String fileName) {
        Struct.Builder inner = Struct.newBuilder();
        inner.putFields("$eq", Value.newBuilder().setStringValue(fileName).build());

        Struct.Builder outer = Struct.newBuilder();
        outer.putFields("source", Value.newBuilder().setStructValue(inner.build()).build());
        return outer.build();
    }

    private String formatRetrievedContext(List<SearchResult> chunks) {
        StringBuilder builder = new StringBuilder();
        int totalChars = 0;

        for (SearchResult chunk : chunks) {
            if (chunk == null || chunk.getText() == null || chunk.getText().isBlank()) {
                continue;
            }

            String text = chunk.getText().trim();
            if (totalChars + text.length() > MAX_COMPARE_CHARS_PER_DOCUMENT) {
                int remaining = MAX_COMPARE_CHARS_PER_DOCUMENT - totalChars;
                if (remaining <= 0) {
                    break;
                }
                text = text.substring(0, remaining);
            }

            builder.append("Chunk (score=")
                    .append(String.format(Locale.ROOT, "%.3f", chunk.getScore()))
                    .append("):\n")
                    .append(text)
                    .append("\n\n");
            totalChars += text.length();

            if (totalChars >= MAX_COMPARE_CHARS_PER_DOCUMENT) {
                break;
            }
        }

        return builder.toString().trim();
    }

    private String ingestFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            return "Skipped missing folder: " + folderPath;
        }

        try {
            DocumentIngestionService.ingestFolder(folderPath, vectorStore, embeddingService);
            return "Ingested folder: " + folderPath;
        } catch (Exception e) {
            return "Failed ingesting " + folderPath + ": " + e.getMessage();
        }
    }

    private DocumentInfo toDocumentInfo(String folder, Path path) {
        DocumentInfo info = new DocumentInfo();
        info.setDocumentId(buildDocumentId(folder, path.getFileName().toString()));
        info.setFileName(path.getFileName().toString());
        info.setFolder(folder);
        info.setDocumentType(getDocType(path.getFileName().toString()));
        info.setSize(safeSize(path));
        info.setLastModified(new Date(safeLastModified(path)));
        return info;
    }

    private String buildDocumentId(String folder, String fileName) {
        return folder + "/" + fileName;
    }

    private Path resolveManagedFolder(String folder) {
        if (!DOCUMENT_FOLDERS.contains(folder)) {
            throw new IllegalArgumentException("Unsupported folder: " + folder);
        }
        Path directory = DATA_ROOT.resolve(folder).normalize();
        ensureUnderRoot(directory, DATA_ROOT);
        return directory;
    }

    private void ensureUnderRoot(Path candidate, Path root) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(normalizedRoot)) {
            throw new IllegalArgumentException("Invalid path");
        }
    }

    private boolean isSupportedDocument(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".txt");
    }

    private long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private long safeLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private String sanitizeFileName(String originalName) {
        if (isBlank(originalName)) {
            throw new IllegalArgumentException("File name is required");
        }

        try {
            String fileName = Paths.get(originalName).getFileName().toString();
            String sanitized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
            if (isBlank(sanitized)) {
                throw new IllegalArgumentException("Invalid file name");
            }
            return sanitized;
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid file name", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String getDocType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);

        if (lower.contains("contract")) {
            return "contract";
        }
        if (lower.contains("invoice")) {
            return "invoice";
        }
        if (lower.contains("policy")) {
            return "policy";
        }
        if (lower.contains("mail") || lower.contains("log")) {
            return "log";
        }

        return "unknown";
    }

    private record ManagedDocument(
            String documentId,
            String folder,
            String fileName,
            String documentType) {
    }
}
