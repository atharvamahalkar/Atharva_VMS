# Vendor-Managment-using-Java



1. Install Ollama
2. Pull qwen:0.5b
3. Run "ollama serve"
3. Add your own pinecone api_key in Config.java
4. Install Maven Zip File
5. Extract it.
6. Add the bin folder to environment variables.
7. open cmd go to this folder.
8. run "mvn clean install" to install dependencies.
9. then run "mvn clean compile"
10. then "mvn exec:java"

⚙️ Setup & Run Instructions (For New Users)
🔹 1. Clone the Repository
git clone https://github.com/your-username/your-repo-name.git
cd your-repo-name
🔹 2. Install Prerequisites

Make sure you have:

Java 17+
Maven
PostgreSQL
Ollama installed
Pinecone account (API key)
🔹 3. Setup PostgreSQL Database
Step 1: Open pgAdmin

Create a new database:

vendor_rag_db
Step 2: Run Schema SQL

Open Query Tool and run:

-- Example (simplified)

CREATE TABLE vendors (
    vendor_id SERIAL PRIMARY KEY,
    vendor_name VARCHAR(255)
);

CREATE TABLE contracts (
    contract_id VARCHAR(50) PRIMARY KEY,
    vendor_id INT REFERENCES vendors(vendor_id)
);

👉 (Use full schema from project if available)

🔹 4. Configure Database in Spring Boot

Go to:

src/main/resources/application.properties

Add:

spring.datasource.url=jdbc:postgresql://localhost:5432/vendor_rag_db
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
🔹 5. Setup Ollama

Run:

ollama serve
ollama pull nomic-embed-text
ollama pull qwen:0.5b
🔹 6. Setup Pinecone
Create an index (dimension: 768)
Copy API key

Update your config file:

Config.java
PINECONE_API_KEY = "your_key";
INDEX_NAME = "your_index";
NAMESPACE = "vendor-docs";
🔹 7. Add Documents

Place files inside:

data/
 ├── contracts/
 ├── invoices/
 ├── policies/
 └── logs/

Example:

data/contracts/vendor1_contract.txt
data/invoices/vendor2_invoice.txt
🔹 8. Build the Project
mvn clean install
🔹 9. Run the Application
mvn spring-boot:run
🔹 10. Ingest Documents

Inside the app (CLI or code), run:

ingest

👉 This will:

Read files
Create chunks
Store in Pinecone
Save metadata in DB
🔹 11. Ask Queries

Example queries:

What are the payment terms in contract C-2001?
What is the total amount for invoice INV-2001?
What is the invoice amount and payment terms for contract C-2001?
🧪 Verify Setup
In pgAdmin:
SELECT * FROM vendors;
SELECT * FROM contracts;
In terminal:
Check logs for retrieval
Check LLM responses


The system combines:

- **PostgreSQL** → structured data storage  
- **Pinecone** → semantic vector search  
- **Ollama (LLM)** → intelligent answer generation  

---

## 🎯 Objective

To build an AI-powered system that can:

- Understand vendor documents  
- Retrieve relevant information  
- Answer user queries accurately  
- Combine structured + unstructured data  

---

## 🧠 Architecture


User Query
↓
Embedding (Ollama)
↓
Pinecone (Vector Search)
↓
Top Relevant Chunks
↓
Context + Prompt
↓
LLM (Ollama - qwen)
↓
Final Answer


---

## 🧱 Tech Stack

| Layer | Technology |
|------|-----------|
| Backend | Java (Spring Boot) |
| Database | PostgreSQL |
| Vector DB | Pinecone |
| LLM | Ollama (`qwen:0.5b`) |
| Embedding Model | `nomic-embed-text` |
| Build Tool | Maven |

---

## 📁 Project Structure


data/
├── contracts/
├── invoices/
├── policies/
└── logs/

src/main/java/com/example/rag/
├── config/
├── entity/
├── repository/
├── ingestion/
├── embedding/
├── vectorstore/
├── llm/
└── pipeline/


---

## ⚙️ Features Implemented

### ✅ Tier 1 – Embeddings
- Integrated Ollama
- Using `nomic-embed-text` (768 dimensions)

### ✅ Tier 2 – Vector Database (Pinecone)
- Index creation and configuration
- Vector upsert and query
- Fixed dimension mismatch issue

### ✅ Tier 3 – Database Layer (PostgreSQL)
- Created database: `vendor_rag_db`
- Tables implemented:
  - `vendors`
  - `contracts`
  - `contract_sections`
  - `documents`
  - `document_chunks`
  - `invoices`
  - `deliveries`
  - `communication_logs`
- JPA Entities and Repositories created
- Spring Boot successfully connected to PostgreSQL

### ✅ Ingestion Pipeline
- Reads `.txt` documents
- Extracts:
  - Vendor name
  - Contract / Invoice IDs
  - Sections
- Splits into chunks
- Stores:
  - Pinecone → for semantic retrieval
  - PostgreSQL → for structured storage

### ✅ Retrieval System
- Fetches relevant chunks from Pinecone
- Passes context to LLM
- Generates answers using Ollama

---

## ⚠️ Issues Faced & Fixes

### ❌ Ollama not running
✔ Fixed by:
```bash
ollama serve
ollama pull nomic-embed-text
