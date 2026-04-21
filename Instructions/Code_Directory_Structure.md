# Code Directory Structure — Architectural Ledger
# Language: Java (JDK 11+)  |  No Python required
# All RAG calls made via Java HTTP to Ollama & Pinecone REST APIs

```
ArchitecturalLedger/
├── 📁 data
│   ├── 📁 contracts
│   │   ├── 📄 vendor1_contract.txt
│   │   └── 📄 vendor2_contract.txt
│   ├── 📁 invoices
│   │   ├── 📄 vendor1_invoice.txt
│   │   └── 📄 vendor2_invoice.txt
│   ├── 📁 logs
│   │   ├── 📄 vendor_mail_1.txt
│   │   └── 📄 vendor_mail_2.txt
│   ├── 📁 policies
│   │   ├── 📄 vendor2_policy.txt
│   │   └── 📄 vendor_policy.txt
│   └── 📁 uploads
├── 📁 src
│   ├── 📁 main
│   │   ├── 📁 java
│   │   │   └── 📁 com
│   │   │       └── 📁 example
│   │   │           └── 📁 rag
│   │   │               ├── 📁 api
│   │   │               │   ├── ☕ ComparisonRequest.java
│   │   │               │   ├── ☕ ComparisonResponse.java
│   │   │               │   ├── ☕ DocumentInfo.java
│   │   │               │   ├── ☕ DocumentListResponse.java
│   │   │               │   ├── ☕ IngestResponse.java
│   │   │               │   ├── ☕ QueryRequest.java
│   │   │               │   ├── ☕ QueryResponse.java
│   │   │               │   ├── ☕ RagController.java
│   │   │               │   └── ☕ RagService.java
│   │   │               ├── 📁 config
│   │   │               │   └── ☕ Config.java
│   │   │               ├── 📁 embedding
│   │   │               │   ├── ☕ EmbeddingProvider.java
│   │   │               │   ├── ☕ OllamaEmbedding.java
│   │   │               │   ├── ☕ OllamaEmbeddingService.java
│   │   │               │   └── ☕ OpenAIEmbedding.java
│   │   │               ├── 📁 entity
│   │   │               │   ├── ☕ Contract.java
│   │   │               │   ├── ☕ ContractSection.java
│   │   │               │   ├── ☕ Document.java
│   │   │               │   ├── ☕ DocumentChunk.java
│   │   │               │   └── ☕ Vendor.java
│   │   │               ├── 📁 examples
│   │   │               │   └── ☕ MinimalUpsertAndQueryExample.java
│   │   │               ├── 📁 ingestion
│   │   │               │   ├── ☕ ChunkingService.java
│   │   │               │   ├── ☕ DocumentIngestionService.java
│   │   │               │   ├── ☕ DocumentReaderService.java
│   │   │               │   └── ☕ EmbeddingService.java
│   │   │               ├── 📁 llm
│   │   │               │   ├── ☕ CloudLLM.java
│   │   │               │   ├── ☕ LLMProvider.java
│   │   │               │   └── ☕ OllamaLLM.java
│   │   │               ├── 📁 pipeline
│   │   │               │   └── ☕ RAGPipeline.java
│   │   │               ├── 📁 repository
│   │   │               │   ├── ☕ ContractRepository.java
│   │   │               │   ├── ☕ ContractSectionRepository.java
│   │   │               │   ├── ☕ DocumentChunkRepository.java
│   │   │               │   ├── ☕ DocumentRepository.java
│   │   │               │   └── ☕ VendorRepository.java
│   │   │               ├── 📁 util
│   │   │               │   └── ☕ PromptBuilder.java
│   │   │               ├── 📁 vectorstore
│   │   │               │   ├── ☕ Document.java
│   │   │               │   ├── ☕ IndexInitializer.java
│   │   │               │   ├── ☕ OllamaChatService.java
│   │   │               │   ├── ☕ SearchResult.java
│   │   │               │   └── ☕ VectorStore.java
│   │   │               ├── ☕ Main.java
│   │   │               ├── ☕ RagApplication.java
│   │   │               └── ⚙️ doc.json
│   │   └── 📁 resources
│   │       ├── 📁 static
│   │       │   ├── 📄 app.js
│   │       │   ├── 🌐 comparison.html
│   │       │   ├── 🌐 index.html
│   │       │   ├── 🌐 repository.html
│   │       │   └── 🎨 style.css
│   │       ├── 📄 application.properties
│   │       ├── 📄 knowledge2.txt
│   │       └── ⚙️ logback.xml
│   └── 📁 test
│       └── 📁 java
│           └── 📁 com
│               └── 📁 example
│                   └── 📁 rag
│                       └── 📁 integration
│                           └── ☕ RAGIntegrationTest.java
├── 📁 stitch_vendor_management_system
│   └── 📁 stitch_vendor_management_system
│       ├── 📁 document_comparison_upload_sync
│       │   ├── 🌐 code.html
│       │   └── 🖼️ screen.png
│       ├── 📁 login_page
│       │   ├── 🌐 code.html
│       │   └── 🖼️ screen.png
│       ├── 📁 sample_contracts_library_upload_sync
│       │   ├── 🌐 code.html
│       │   └── 🖼️ screen.png
│       ├── 📁 search_first_dashboard_upload_sync
│       │   ├── 🌐 code.html
│       │   └── 🖼️ screen.png
│       └── 📁 structure_slate
│           └── 📝 DESIGN.md
├── ⚙️ .dockerignore
├── ⚙️ .env.example
├── 📝 DEPLOYMENT.md
├── 🐳 Dockerfile
├── 📝 README.md
├── 📄 build-and-run.bat
├── ⚙️ compose.yaml
├── 📄 cp.txt
└── ⚙️ pom.xml
```

## Key Java Classes — AI Layer

| Class | Role |
|---|---|
| OllamaClient.java | HTTP POST to Ollama /api/embeddings and /api/generate |
| PineconeClient.java | HTTP POST to Pinecone /vectors/upsert and /query |
| IngestService.java | Chunk → embed → upsert pipeline |
| SearchService.java | Embed query → Pinecone → Qwen generate |
| CompareService.java | Dual retrieval → comparison prompt → Qwen |
| TextChunker.java | 500-token chunking with 50-token overlap |
