# Deployment Guide

## What This App Needs

- Java 17
- Maven 3.9+
- PostgreSQL
- Ollama running with:
  - `qwen:0.5b`
  - `nomic-embed-text`
- Pinecone index with dimension `768`

## Environment Variables

Copy `.env.example` to `.env` and set real values:

```powershell
Copy-Item .env.example .env
```

Important variables:

- `PINECONE_API_KEY`
- `PINECONE_INDEX_NAME`
- `PINECONE_NAMESPACE`
- `OLLAMA_BASE_URL`
- `OLLAMA_MODEL`
- `OLLAMA_EMBEDDING_MODEL`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `PORT`

## Local Run

1. Start PostgreSQL and create database `vendor_rag_db`.
2. Start Ollama:

```powershell
ollama serve
ollama pull qwen:0.5b
ollama pull nomic-embed-text
```

3. Export environment variables or load them from `.env`.
4. Run the app:

```powershell
mvn spring-boot:run
```

The app will be available at `http://localhost:8080`.

## Docker Compose Run

`compose.yaml` starts:

- PostgreSQL
- Spring Boot application

Ollama is expected to run outside Docker and is reached through `OLLAMA_BASE_URL`.

Steps:

1. Copy `.env.example` to `.env`
2. Set your real Pinecone key in `.env`
3. Make sure Ollama is already running on the host
4. Start the stack:

```powershell
docker compose up --build
```

## First-Time Ingestion

After the app starts, ingest the sample documents:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/ingest
```

## Useful Endpoints

- `GET /api/documents`
- `POST /api/ingest`
- `POST /api/query`
- `POST /api/compare`
- `POST /api/upload`

## Deployment Notes

- The application now reads Pinecone and Ollama settings from environment variables instead of hardcoded values.
- Uploaded files are stored under `data/uploads`.
- The integration test now points to `src/main/resources/knowledge2.txt`.
