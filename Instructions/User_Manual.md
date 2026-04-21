# User Manual — Architectural Ledger VMS Portal

## System Requirements
- OS: Windows 10/11
- RAM: 8 GB minimum
- Browser: Chrome, Firefox, Edge (latest)
- Internet: Required for Pinecone API

## Starting the System
```
1. ollama serve                   (Ollama on port 11434)
2. PostgreSQL service             (auto-starts on port 5432)
3. Run mvn spring-boot:run in terminal     (portal on port 8080)
4. Open http://localhost:8080
```

## Uploading Documents
1. Click **Ingest Documents**
2. Select a vendor document (.txt)
3. Java IngestService chunks → embeds via Ollama → upserts to Pinecone
4. Document appears in Repository

## AI Search
1. Click **Search**
2. Type a natural language question:
   - "What are the payment terms in vendor2_contract?"
   - "What is the penalty for late delivery?"
3. Click **Execute** — Qwen:0.5B answers in 2–5 seconds

## Document Comparison
1. Click **Compare**
2. Select Sample Contract (reference) + Your Contract
3. Click **Compare Documents**
4. AI extracts: payment terms, delivery terms, penalty clauses

## Vendor Management
| Action | Steps |
|--------|-------|
| Add    | Vendors → Add New → Fill form → Save |
| Edit   | Select row → Edit → Update |
| Delete | Select row → Delete |

## Troubleshooting
| Issue | Fix |
|-------|-----|
| Slow first answer | Qwen cold start (10–15s) — normal, then 2–5s |
| "Ollama not found" | Run `ollama serve` |
| "Pinecone error" | Check PINECONE_API_KEY in config.properties |
| PostgreSQL error | Verify PostgreSQL is running: `pg_isready -U postgres` |
