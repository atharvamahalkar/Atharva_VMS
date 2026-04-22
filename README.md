# 🏢 Vendor Management System with RAG

An AI-powered Vendor Management System using Retrieval Augmented Generation (RAG) to intelligently analyze and compare vendor contracts, invoices, and documents.

---

## 🚀 Quick Start Guide

Follow these steps to set up and run the application:

### Prerequisites
- Maven 3.9+
- Docker Desktop
- Ollama installed on your host machine
- Pinecone account with API key

### Step-by-Step Setup

#### 1️⃣ Configure Maven
- Download Maven from the [official website](https://maven.apache.org/download.cgi)
- Extract the archive
- Add the `bin` folder path to your system's PATH environment variables

#### 2️⃣ Install Docker & Ollama
- Install [Docker Desktop](https://www.docker.com/products/docker-desktop)
- Install Ollama Image inside Docker Desktop

#### 3️⃣ Setup Ollama Models
Run the following commands to download the required models:

```bash
ollama pull qwen3.5:0.8b
ollama pull nomic-embed-text
ollama serve
```

Keep this terminal running (Ollama needs to be accessible at `http://localhost:11434`)

#### 4️⃣ Configure Pinecone API Key
1. Go to your [Pinecone account](https://www.pinecone.io)
2. Copy your API key
3. Add to your system environment variables:
   - Variable name: `PINECONE_API_KEY`
   - Variable value: `<your-pinecone-api-key>`

#### 5️⃣ Start PostgreSQL & Application
In a new terminal, navigate to the project root and run:

```bash
docker-compose up --build
```

This will start:
- ✅ PostgreSQL database
- ✅ Spring Boot application on `http://localhost:8080`

#### 6️⃣ Access the Application
Open your browser and go to:

```
http://localhost:8080
```

You'll see the vendor management dashboard with options to:
- 📄 Upload and ingest documents
- 🔍 Search vendor information
- ⚖️ Compare contracts and invoices
- 💬 Ask intelligent questions about your documents

---

## 📁 Project Structure

```
Atharva_VMS/
├── data/                          # Document storage
│   ├── contracts/                 # Vendor contracts
│   ├── invoices/                  # Vendor invoices
│   ├── policies/                  # Vendor policies
│   └── logs/                      # Communication logs
│
├── src/main/
│   ├── java/com/example/rag/
│   │   ├── config/               # Configuration classes
│   │   ├── api/                  # REST controllers & services
│   │   ├── ingestion/            # Document processing pipeline
│   │   ├── embedding/            # Ollama embedding integration
│   │   ├── vectorstore/          # Pinecone vector store
│   │   ├── llm/                  # LLM providers (Ollama, OpenAI)
│   │   ├── entity/               # JPA entities
│   │   └── repository/           # Database repositories
│   │
│   └── resources/
│       ├── static/               # Frontend (HTML, CSS, JS)
│       └── application.properties # Spring Boot config
│
├── compose.yaml                   # Docker Compose configuration
├── Dockerfile                     # Container image definition
└── pom.xml                        # Maven dependencies
```

---

## 🛠️ Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Backend** | Java 17, Spring Boot | REST API & business logic |
| **Database** | PostgreSQL | Structured data storage |
| **Vector DB** | Pinecone | Semantic vector search |
| **Embedding** | Ollama (nomic-embed-text) | Text embeddings (768-dim) |
| **LLM** | Ollama (qwen:0.5b) | Answer generation |
| **Build** | Maven | Dependency & build management |
| **Containerization** | Docker Compose | Multi-container orchestration |
| **Frontend** | HTML5, CSS3, JavaScript | Web interface |

---

## 🧠 How It Works

```
User Query or Document Upload
           ↓
    Embedding Service (Ollama)
           ↓
    Vector Search (Pinecone)
           ↓
  Retrieve Top Relevant Chunks
           ↓
  Build Context-Aware Prompt
           ↓
    LLM Generation (Ollama)
           ↓
    Return Intelligent Response
```

---

## 📊 Key Features

### ✅ Document Management
- Upload and ingest vendor documents (contracts, invoices, policies)
- Automatic chunking and embedding
- Semantic search across documents

### ✅ Intelligent Querying
- Ask natural language questions about vendor documents
- Context-aware retrieval from Pinecone vector database
- Powered by Ollama LLM for accurate answers

### ✅ Document Comparison
- Deep comparison between two contracts
- Highlight key similarities and differences
- Risk assessment and recommendations
- Extract payment terms, delivery terms, penalties

### ✅ Repository Management
- Browse all ingested documents
- Filter by document type and folder
- View document metadata and ingestion status

### ✅ Vector Database Integration
- Pinecone for semantic search
- Configurable namespaces for data isolation
- Automatic index creation and management

---

## 🔧 Configuration

### Environment Variables
Create a `.env` file or set these system variables:

```env
# Pinecone Configuration (Required)
PINECONE_API_KEY=your_api_key_here
PINECONE_INDEX_NAME=vendor-rag-clean
PINECONE_NAMESPACE=vendor-docs
PINECONE_CLOUD=aws
PINECONE_REGION=us-east-1

# Ollama Configuration (Optional - defaults below)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen:0.5b
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
EMBEDDING_DIM=768
```

### PostgreSQL Connection
The `compose.yaml` automatically handles PostgreSQL. Default credentials:
- **Username**: postgres
- **Password**: postgres
- **Database**: vendor_rag_db
- **Port**: 5432

---

## 📝 Usage Examples

### 1. Upload a Document
- Go to **Compare Documents** page
- Click **Upload File** and select a `.txt` file
- File is automatically ingested into Pinecone

### 2. Query Documents
- Go to **Search** page
- Enter your question: *"What are the payment terms?"*
- Get AI-powered answer with source references

### 3. Compare Contracts
- Select two documents from the dropdown
- Click **Compare Documents**
- View detailed differences, risks, and recommendations

### 4. Ingest Folder
- Place documents in `data/` folders
- Visit the **Ingest** endpoint via API
- All documents are processed and indexed

---

## 🧪 Verification Checklist

- [ ] Maven is in PATH: `mvn --version`
- [ ] Docker is running: `docker --version`
- [ ] Ollama is running: `curl http://localhost:11434/api/tags`
- [ ] PostgreSQL container started: `docker ps`
- [ ] Application accessible: `http://localhost:8080`
- [ ] Models available: `ollama list`

---

## ⚠️ Troubleshooting

### Ollama Connection Failed
```bash
# Ensure Ollama is running
ollama serve

# Check if accessible
curl http://localhost:11434/api/tags
```

### Pinecone API Key Error
```bash
# Verify environment variable is set
echo $PINECONE_API_KEY

# On Windows (PowerShell)
$env:PINECONE_API_KEY
```

### Docker Compose Port Already in Use
```bash
# Change ports in compose.yaml or kill existing containers
docker-compose down
docker-compose up --build
```

### Models Not Found
```bash
# Re-download models
ollama pull qwen:0.5b
ollama pull nomic-embed-text
```

---

## 📚 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/query` | Ask a question about documents |
| POST | `/api/compare` | Compare two documents |
| POST | `/api/ingest` | Ingest all documents from `data/` |
| POST | `/api/upload` | Upload a new document |
| GET | `/api/documents` | List all ingested documents |

---

## 🤝 Contributing

Feel free to fork, modify, and submit pull requests!

---

## 📄 License

This project is provided as-is for educational and commercial use.

---

## 🎯 Future Enhancements

- [ ] Support for PDF documents
- [ ] Multi-language support
- [ ] Advanced filtering and search
- [ ] Document versioning
- [ ] Audit logging
- [ ] User authentication and authorization
- [ ] Scheduled document ingestion

---

## 📧 Support

For issues or questions, please open an issue in the repository or contact the development team.

---

**Happy vendor management! 🚀**

```bash
ollama serve
ollama pull nomic-embed-text
