# Vendor Management System Flow

This document explains how the project runs end to end, from the browser UI to the backend services, vector search, and LLM response generation.

## High-Level Architecture

The active runtime path in this repository is:

```text
Browser UI
  -> Spring Boot REST API
  -> RagService
  -> RAGPipeline / DocumentIngestionService
  -> Ollama + Pinecone
  -> Response back to UI
```

At a high level:

- The frontend is served from `src/main/resources/static`.
- The backend is a Spring Boot application.
- Document ingestion creates embeddings and stores chunks in Pinecone.
- Question answering retrieves relevant chunks, builds a prompt, and asks an LLM to generate the answer.
- Document listing is file-system based.
- Comparison uses retrieved chunks from two selected documents and generates a structured comparison.

## 1. Application Startup Flow

The application starts from:

- `src/main/java/com/example/rag/RagApplication.java`

Important lines:

- Line 6: `@SpringBootApplication` enables Spring Boot auto-configuration and component scanning.
- Line 10: `SpringApplication.run(RagApplication.class, args);` boots the web application.

Source:

- `src/main/java/com/example/rag/RagApplication.java:6`
- `src/main/java/com/example/rag/RagApplication.java:10`

### Configuration Loading

The environment and runtime configuration are defined in:

- `src/main/java/com/example/rag/config/Config.java`
- `src/main/resources/application.properties`

Important behavior:

- Line 15 in `Config.java` reads `PINECONE_API_KEY`.
- Lines 37-40 in `Config.java` fail fast if the Pinecone API key is not set.
- Line 20 in `Config.java` resolves the Ollama generation model.
- Line 18 in `Config.java` resolves the Ollama embedding model.
- Lines 3-11 in `application.properties` define PostgreSQL and JPA settings.

Key sources:

- `src/main/java/com/example/rag/config/Config.java:15`
- `src/main/java/com/example/rag/config/Config.java:20`
- `src/main/java/com/example/rag/config/Config.java:37`
- `src/main/resources/application.properties:3`

## 2. Frontend Flow

The frontend pages are static HTML files:

- `src/main/resources/static/index.html`
- `src/main/resources/static/repository.html`
- `src/main/resources/static/comparison.html`

### Search Page

The search UI is defined in `index.html`.

Important lines:

- Line 181: input box for the user question.
- Line 183: search button.
- Lines 316-342: JavaScript event handler that sends the search request.
- Lines 344-354: JavaScript event handler that starts ingestion.

Flow:

1. The user types a question.
2. Clicking `Execute` sends a `POST` request to `/api/query`.
3. The answer returned by the backend is rendered inside the `response` area.

Key sources:

- `src/main/resources/static/index.html:181`
- `src/main/resources/static/index.html:183`
- `src/main/resources/static/index.html:316`
- `src/main/resources/static/index.html:326`

### Repository Page

The repository page loads and renders available documents.

Important lines:

- Lines 183-212: fetch `/api/documents` on page load.
- Lines 191-205: render document cards.
- Lines 215-228: ingest button posts to `/api/ingest`.

Flow:

1. Page opens.
2. Browser requests `/api/documents`.
3. Returned documents are shown as cards with file name, type, size, and modified date.

Key sources:

- `src/main/resources/static/repository.html:183`
- `src/main/resources/static/repository.html:187`
- `src/main/resources/static/repository.html:191`
- `src/main/resources/static/repository.html:215`

### Comparison Page

The comparison page supports:

- loading contract documents
- uploading a `.txt` file
- selecting two documents
- sending a comparison request

Important lines:

- Lines 275-312: load repository documents into dropdowns.
- Lines 280-343: upload file flow.
- Lines 345-402: compare two selected documents.
- Lines 404-416: format comparison result for display.

Key sources:

- `src/main/resources/static/comparison.html:275`
- `src/main/resources/static/comparison.html:314`
- `src/main/resources/static/comparison.html:321`
- `src/main/resources/static/comparison.html:345`
- `src/main/resources/static/comparison.html:368`
- `src/main/resources/static/comparison.html:404`

## 3. API Entry Flow

All REST entry points are handled by:

- `src/main/java/com/example/rag/api/RagController.java`

Base setup:

- Line 15: `@RestController`
- Line 16: `@RequestMapping("/api")`
- Line 17: `@CrossOrigin(origins = "*")`

These annotations make the class the HTTP API entry point for the frontend.

### Query Endpoint

- Line 26: `@PostMapping("/query")`
- Lines 28-30: validate that the question exists.
- Line 32: call `ragService.ask(...)`.
- Line 33: return the answer as `QueryResponse`.

Sources:

- `src/main/java/com/example/rag/api/RagController.java:26`
- `src/main/java/com/example/rag/api/RagController.java:28`
- `src/main/java/com/example/rag/api/RagController.java:32`

### Ingest Endpoint

- Line 36: `@PostMapping("/ingest")`
- Line 39: call `ragService.ingestAll()`
- Lines 41-43: return server error response if ingestion fails.

Sources:

- `src/main/java/com/example/rag/api/RagController.java:36`
- `src/main/java/com/example/rag/api/RagController.java:39`

### Documents Endpoint

- Line 47: `@GetMapping("/documents")`
- Line 49: call `ragService.listDocuments()`

Sources:

- `src/main/java/com/example/rag/api/RagController.java:47`
- `src/main/java/com/example/rag/api/RagController.java:49`

### Compare Endpoint

- Line 55: `@PostMapping("/compare")`
- Lines 58-62: validate both document IDs.
- Line 64: call `ragService.compareDocuments(...)`

Sources:

- `src/main/java/com/example/rag/api/RagController.java:55`
- `src/main/java/com/example/rag/api/RagController.java:58`
- `src/main/java/com/example/rag/api/RagController.java:64`

### Upload Endpoint

- Line 74: `@PostMapping("/upload")`
- Line 77: call `ragService.uploadDocument(file)`

Sources:

- `src/main/java/com/example/rag/api/RagController.java:74`
- `src/main/java/com/example/rag/api/RagController.java:77`

## 4. Service Layer Flow

The orchestration layer is:

- `src/main/java/com/example/rag/api/RagService.java`

This is the main coordinator between the controller, ingestion logic, vector store, and RAG pipeline.

### Lazy Service Initialization

Important lines:

- Line 52: `initializeServices()`
- Lines 53-60: create `VectorStore`, `RAGPipeline`, `OllamaEmbeddingService`, and comparison LLM only once.
- Lines 57-59: fail if Ollama is not available.

Sources:

- `src/main/java/com/example/rag/api/RagService.java:52`
- `src/main/java/com/example/rag/api/RagService.java:53`
- `src/main/java/com/example/rag/api/RagService.java:57`
- `src/main/java/com/example/rag/api/RagService.java:60`

### Ask Flow

- Line 64: `ask(String question)`
- Line 65: initialize services.
- Line 66: delegate to `ragPipeline.ask(question)`.

This method is the main bridge from the REST API into the RAG question-answering path.

Sources:

- `src/main/java/com/example/rag/api/RagService.java:64`
- `src/main/java/com/example/rag/api/RagService.java:65`
- `src/main/java/com/example/rag/api/RagService.java:66`

### Ingest-All Flow

- Line 69: `ingestAll()`
- Lines 73-79: iterate through configured document folders.
- Line 77: call `ingestFolder(...)`.

This is the entry point used by the UI to ingest the managed data folders.

Sources:

- `src/main/java/com/example/rag/api/RagService.java:69`
- `src/main/java/com/example/rag/api/RagService.java:73`
- `src/main/java/com/example/rag/api/RagService.java:77`

### List Documents Flow

- Line 84: `listDocuments()`
- Lines 87-102: scan the managed folders and collect supported files.
- Line 97: convert each file to `DocumentInfo`.

This endpoint is file-system based, not Pinecone based.

Sources:

- `src/main/java/com/example/rag/api/RagService.java:84`
- `src/main/java/com/example/rag/api/RagService.java:87`
- `src/main/java/com/example/rag/api/RagService.java:97`

### Upload Flow

- Line 149: `uploadDocument(MultipartFile file)`
- Lines 150-157: validate non-empty `.txt` files only.
- Lines 159-166: save file under `data/uploads`.
- Lines 168-169: build and return a document ID.

Important note:

The upload endpoint stores the file, but does not ingest it into Pinecone by itself.

Sources:

- `src/main/java/com/example/rag/api/RagService.java:149`
- `src/main/java/com/example/rag/api/RagService.java:155`
- `src/main/java/com/example/rag/api/RagService.java:159`
- `src/main/java/com/example/rag/api/RagService.java:168`

### Compare Flow

- Line 107: `compareDocuments(String document1Id, String document2Id)`
- Lines 110-111: resolve document IDs into managed files.
- Line 115: embed a fixed comparison query.
- Lines 117-126: search Pinecone for relevant chunks from each specific source file.
- Lines 128-139: return a fallback message if either document is not found in indexed context.
- Line 142: build structured comparison prompt.
- Line 143: generate the final comparison using the LLM.

Sources:

- `src/main/java/com/example/rag/api/RagService.java:107`
- `src/main/java/com/example/rag/api/RagService.java:110`
- `src/main/java/com/example/rag/api/RagService.java:115`
- `src/main/java/com/example/rag/api/RagService.java:117`
- `src/main/java/com/example/rag/api/RagService.java:128`
- `src/main/java/com/example/rag/api/RagService.java:142`
- `src/main/java/com/example/rag/api/RagService.java:143`

## 5. Question Answering Flow

The core retrieval-augmented generation logic is implemented in:

- `src/main/java/com/example/rag/pipeline/RAGPipeline.java`

### Constructor Flow

- Lines 26-38: choose the embedding provider and LLM provider based on config.

If `OLLAMA_MODEL` is present, the system uses:

- `OllamaEmbedding`
- `OllamaLLM`

Otherwise it falls back to:

- `OpenAIEmbedding`
- `CloudLLM`

Sources:

- `src/main/java/com/example/rag/pipeline/RAGPipeline.java:26`
- `src/main/java/com/example/rag/pipeline/RAGPipeline.java:29`
- `src/main/java/com/example/rag/pipeline/RAGPipeline.java:32`
- `src/main/java/com/example/rag/pipeline/RAGPipeline.java:36`

### ask() Execution Flow

Important lines:

- Line 43: create embedding for the user question.
- Line 44: search the vector store for top matches.
- Lines 50-62: filter valid chunks and build context text.
- Line 70: build prompt.
- Line 72: generate answer with the selected LLM provider.
- Line 78: return final answer.

The prompt itself is defined on lines 81-99 and explicitly instructs the model to answer only from retrieved context.

Sources:

- `src/main/java/com/example/rag/pipeline/RAGPipeline.java:43`
- `src/main/java/com/example/rag/pipeline/RAGPipeline.java:44`
- `src/main/java/com/example/rag/pipeline/RAGPipeline.java:50`
- `src/main/java/com/example/rag/pipeline/RAGPipeline.java:70`
- `src/main/java/com/example/rag/pipeline/RAGPipeline.java:72`
- `src/main/java/com/example/rag/pipeline/RAGPipeline.java:81`

## 6. Embedding Flow

The active embedding client used in this web flow is:

- `src/main/java/com/example/rag/embedding/OllamaEmbeddingService.java`

Important lines:

- Line 33: `generateEmbedding(String text)`
- Line 34: target endpoint is `baseUrl + "/api/embeddings"`
- Lines 41-42: retry state setup.
- Lines 45-52: send HTTP request to Ollama.
- Lines 69-79: convert response array to `float[]` and validate dimension.
- Lines 102-115: `isAvailable()` checks `/api/tags`.

Sources:

- `src/main/java/com/example/rag/embedding/OllamaEmbeddingService.java:33`
- `src/main/java/com/example/rag/embedding/OllamaEmbeddingService.java:34`
- `src/main/java/com/example/rag/embedding/OllamaEmbeddingService.java:41`
- `src/main/java/com/example/rag/embedding/OllamaEmbeddingService.java:45`
- `src/main/java/com/example/rag/embedding/OllamaEmbeddingService.java:69`
- `src/main/java/com/example/rag/embedding/OllamaEmbeddingService.java:102`

## 7. Vector Store Flow

Vector storage and retrieval are handled by:

- `src/main/java/com/example/rag/vectorstore/VectorStore.java`

### Initialization

- Line 26: constructor starts.
- Line 28: create Pinecone client.
- Line 32: ensure index exists.
- Line 38: open connection to the Pinecone index.

Sources:

- `src/main/java/com/example/rag/vectorstore/VectorStore.java:26`
- `src/main/java/com/example/rag/vectorstore/VectorStore.java:28`
- `src/main/java/com/example/rag/vectorstore/VectorStore.java:32`
- `src/main/java/com/example/rag/vectorstore/VectorStore.java:38`

### Upsert Flow

- Line 69: `upsert(id, content, embedding, metadata)`
- Lines 71-89: build metadata payload including `text` and `content`.
- Line 90: upsert vector into Pinecone namespace.
- Line 91: store local cache fallback.

Sources:

- `src/main/java/com/example/rag/vectorstore/VectorStore.java:69`
- `src/main/java/com/example/rag/vectorstore/VectorStore.java:71`
- `src/main/java/com/example/rag/vectorstore/VectorStore.java:90`
- `src/main/java/com/example/rag/vectorstore/VectorStore.java:91`

### Search Flow

Detailed search used by the RAG pipeline and comparison flow:

- Line 195: `searchWithScores(...)`
- Lines 199-209: query Pinecone with metadata included.
- Lines 216-273: extract content, metadata, and score into `SearchResult`.

Sources:

- `src/main/java/com/example/rag/vectorstore/VectorStore.java:195`
- `src/main/java/com/example/rag/vectorstore/VectorStore.java:199`
- `src/main/java/com/example/rag/vectorstore/VectorStore.java:216`

### Delete-by-Source Flow

- Line 295: `deleteBySource(String sourceFileName)`
- Lines 300-306: build metadata filter and delete all vectors with the same source file.

This is what allows re-ingestion of a file without leaving stale chunks in the vector database.

Sources:

- `src/main/java/com/example/rag/vectorstore/VectorStore.java:295`
- `src/main/java/com/example/rag/vectorstore/VectorStore.java:300`
- `src/main/java/com/example/rag/vectorstore/VectorStore.java:306`

## 8. Pinecone Index Initialization Flow

Index creation is handled by:

- `src/main/java/com/example/rag/vectorstore/IndexInitializer.java`

Important lines:

- Line 18: `createIndexIfNotExists()`
- Line 19: check whether the index exists.
- Lines 21-31: if it exists, validate embedding dimension.
- Lines 38-44: create serverless Pinecone index if needed.
- Line 53: wait until index becomes ready.

Sources:

- `src/main/java/com/example/rag/vectorstore/IndexInitializer.java:18`
- `src/main/java/com/example/rag/vectorstore/IndexInitializer.java:19`
- `src/main/java/com/example/rag/vectorstore/IndexInitializer.java:21`
- `src/main/java/com/example/rag/vectorstore/IndexInitializer.java:38`
- `src/main/java/com/example/rag/vectorstore/IndexInitializer.java:53`

## 9. Document Ingestion Flow

The ingestion pipeline is implemented in:

- `src/main/java/com/example/rag/ingestion/DocumentIngestionService.java`

### Folder Ingestion

Important lines:

- Line 15: `ingestFolder(...)`
- Lines 19-20: read the target folder.
- Lines 27-28: iterate through files.
- Line 34: read content using `DocumentReaderService`.
- Line 41: chunk content with `ChunkingService`.
- Line 45: delete previous vectors for the same source file.
- Line 52: generate embedding for each chunk.
- Line 61: upsert chunk and metadata into Pinecone.

Sources:

- `src/main/java/com/example/rag/ingestion/DocumentIngestionService.java:15`
- `src/main/java/com/example/rag/ingestion/DocumentIngestionService.java:34`
- `src/main/java/com/example/rag/ingestion/DocumentIngestionService.java:41`
- `src/main/java/com/example/rag/ingestion/DocumentIngestionService.java:45`
- `src/main/java/com/example/rag/ingestion/DocumentIngestionService.java:52`
- `src/main/java/com/example/rag/ingestion/DocumentIngestionService.java:61`

### File Reading

The document reader is:

- `src/main/java/com/example/rag/ingestion/DocumentReaderService.java`

Important lines:

- Line 14: if the file is a PDF, use PDFBox to extract text.
- Line 21: otherwise read the file as plain text.

Sources:

- `src/main/java/com/example/rag/ingestion/DocumentReaderService.java:14`
- `src/main/java/com/example/rag/ingestion/DocumentReaderService.java:21`

### Chunking

Chunking is implemented in:

- `src/main/java/com/example/rag/ingestion/ChunkingService.java`

Important lines:

- Line 8: `chunkText(String text, int chunkSize)`
- Line 15: loop through the text in fixed-size character windows.
- Line 16: slice each chunk.

Sources:

- `src/main/java/com/example/rag/ingestion/ChunkingService.java:8`
- `src/main/java/com/example/rag/ingestion/ChunkingService.java:15`
- `src/main/java/com/example/rag/ingestion/ChunkingService.java:16`

## 10. LLM Generation Flow

The Ollama text generation client is:

- `src/main/java/com/example/rag/llm/OllamaLLM.java`

Important lines:

- Line 16: `generate(String prompt)`
- Lines 38-43: build payload with model, prompt, and `stream=false`.
- Lines 45-49: send HTTP request to `/api/generate`.
- Lines 51-72: retry logic.
- Lines 97-118: safely extract the generated content from different response shapes.

Sources:

- `src/main/java/com/example/rag/llm/OllamaLLM.java:16`
- `src/main/java/com/example/rag/llm/OllamaLLM.java:38`
- `src/main/java/com/example/rag/llm/OllamaLLM.java:45`
- `src/main/java/com/example/rag/llm/OllamaLLM.java:51`
- `src/main/java/com/example/rag/llm/OllamaLLM.java:97`

## 11. End-to-End Runtime Flows

### Search Flow Summary

```text
index.html
  -> POST /api/query
  -> RagController.query()
  -> RagService.ask()
  -> RAGPipeline.ask()
  -> OllamaEmbeddingService.generateEmbedding()
  -> VectorStore.searchWithScores()
  -> OllamaLLM.generate()
  -> QueryResponse
  -> index.html renders answer
```

### Ingestion Flow Summary

```text
index.html / repository.html
  -> POST /api/ingest
  -> RagController.ingest()
  -> RagService.ingestAll()
  -> DocumentIngestionService.ingestFolder()
  -> DocumentReaderService.readFile()
  -> ChunkingService.chunkText()
  -> OllamaEmbeddingService.generateEmbedding()
  -> VectorStore.upsert()
```

### Comparison Flow Summary

```text
comparison.html
  -> POST /api/compare
  -> RagController.compareDocuments()
  -> RagService.compareDocuments()
  -> OllamaEmbeddingService.generateEmbedding()
  -> VectorStore.searchWithScores(filter by source)
  -> RagService.buildRagComparisonPrompt()
  -> OllamaLLM.generate()
  -> comparison.html formats result
```

## 12. Important Observations

### What Is Actually Used in the Current Web Flow

The current live web flow uses:

- static HTML pages
- `RagController`
- `RagService`
- `RAGPipeline`
- `DocumentIngestionService`
- `OllamaEmbeddingService`
- `VectorStore`
- `OllamaLLM`

### What Exists but Is Not Part of the Main Web Flow

There is also:

- `src/main/java/com/example/rag/Main.java`
- entity classes under `src/main/java/com/example/rag/entity`
- repository interfaces under `src/main/java/com/example/rag/repository`

These exist in the repository, but they are not part of the active `RagController -> RagService -> RAGPipeline` web request path.

### Important Limitation

The upload flow saves files into `data/uploads`, but uploaded files are not automatically ingested into Pinecone. That means:

1. Upload can succeed.
2. Document listing can show the file.
3. Comparison or search may still fail if that uploaded content is not indexed.

Relevant source:

- `src/main/java/com/example/rag/api/RagService.java:149`
- `src/main/java/com/example/rag/api/RagService.java:166`
- `src/main/java/com/example/rag/api/RagService.java:168`

## 13. Final One-Line Flow

If you need the entire project flow in one line, it is:

```text
User action in HTML page -> JavaScript fetch call -> RagController endpoint -> RagService orchestration -> embedding/search/ingestion logic -> Pinecone/Ollama -> formatted response back to UI
```
