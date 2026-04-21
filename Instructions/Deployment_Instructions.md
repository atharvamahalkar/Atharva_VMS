# Deployment Instructions — Architectural Ledger VMS Portal
# Stack: Java (JDK 11+) · Qwen:0.5B (Ollama) · Pinecone · nomic-embed-text · PostgreSQL 15
# NOTE: No Python required — all AI calls are made directly from Java via HTTP

---

## Prerequisites
- Java JDK 11+
- PostgreSQL 15
- Internet connection (for Pinecone API)
- RAM: 4 GB minimum (Qwen:0.5B uses ~1.2 GB)

---

## Step 1 — Install Ollama & Pull Models

```bash
# Linux / macOS
curl -fsSL https://ollama.com/install.sh | sh

# Windows: Download from https://ollama.com/download

# Pull models (once only)
ollama pull qwen:0.5b          # ~350 MB
ollama pull nomic-embed-text   # ~270 MB

# Start server
ollama serve
# Runs at http://localhost:11434
```

---

## Step 2 — Configure Pinecone

1. Sign up free at https://app.pinecone.io
2. Create Index:
   - Name: `architectural-ledger`
   - Dimensions: `768`  ← must match nomic-embed-text
   - Metric: `cosine`
   - Type: Serverless (free)
3. Copy your API Key

4. Update `config.properties` in the project root:
```properties
OLLAMA_BASE_URL=http://localhost:11434
LLM_MODEL=qwen:0.5b
EMBED_MODEL=nomic-embed-text
PINECONE_API_KEY=your-pinecone-api-key
PINECONE_INDEX=architectural-ledger
CHUNK_SIZE=500
CHUNK_OVERLAP=50
TOP_K=5
```

---

## Step 3 — Set Up PostgreSQL

```bash
# Create database
createdb -U postgres vendor_management

# Run schema
psql -U postgres -d vendor_management -f database/schema.sql

# Verify
psql -U postgres -d vendor_management -c "\dt"
# Expected tables: users, vendors, documents
```

Update `src/com/vms/util/DBConnection.java`:
```java
private static final String URL    = "jdbc:postgresql://localhost:5432/vendor_management";
private static final String USER   = "postgres";
private static final String PASS   = "your_postgres_password";
private static final String DRIVER = "org.postgresql.Driver";
```

---

## Step 4 — Add Java Dependencies to Build Path

Download and add to project build path:
- `postgresql-42.x.x.jar` — https://jdbc.postgresql.org/download/
- `org.json-20240303.jar` — https://mvnrepository.com/artifact/org.json/json
## 📋 Direct Dependencies (from pom.xml)

| Dependency | Version | Purpose | Maven Central Link |
|------------|---------|---------|--------------------|
| spring-boot-starter | 3.2.5 | Core Spring Boot | https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter/3.2.5 |
| spring-boot-starter-web | 3.2.5 | Web/REST APIs | https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web/3.2.5 |
| spring-boot-starter-data-jpa | 3.2.5 | JPA/Hibernate | https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-jpa/3.2.5 |
| postgresql | 42.6.2 | PostgreSQL Driver | https://mvnrepository.com/artifact/org.postgresql/postgresql/42.6.2 |
| elasticsearch-java | 8.10.4 | Elasticsearch Client | https://mvnrepository.com/artifact/co.elastic.clients/elasticsearch-java/8.10.4 |
| okhttp | 4.12.0 | HTTP Client | https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp/4.12.0 |
| jackson-databind | 2.15.4 | JSON Processing | https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind/2.15.4 |
| pinecone-client | 3.0.0 | Pinecone Vector DB | https://mvnrepository.com/artifact/io.pinecone/pinecone-client/3.0.0 |
| protobuf-java | 3.25.1 | Protocol Buffers | https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java/3.25.1 |
| pdfbox | 2.0.29 | PDF Processing | https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox/2.0.29 |
| json | 20240303 | JSON Library | https://mvnrepository.com/artifact/org.json/json/20240303 |
| spring-boot-starter-test | 3.2.5 | Testing | https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-test/3.2.5 |

---

## Step 5 — Run the Application

```bash
# Go to the source folder and run the below bash command.
mvn spring-boot:run
```

Open browser → **http://localhost:8080**
Login: **admin / admin123**

---

## Startup Checklist

| Service         | Command                  | Port  |
|-----------------|--------------------------|-------|
| Ollama          | `ollama serve`           | 11434 |
| PostgreSQL      | Auto-starts as OS service| 5432  |
| Java App        | Run `Main.java`    | 8080  |

---

## Troubleshooting

| Error | Fix |
|-------|-----|
| `Connection refused localhost:11434` | Run `ollama serve` |
| `qwen:0.5b not found` | `ollama pull qwen:0.5b` |
| `PineconeException: Unauthorized` | Check PINECONE_API_KEY in config.properties |
| `Dimension mismatch` | Recreate Pinecone index with dimensions=768 |
| `PSQLException: Connection refused` | Start PostgreSQL service |
| `ClassNotFoundException: org.postgresql.Driver` | Add postgresql jar to build path |