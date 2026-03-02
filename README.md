# LynorAI - Enterprise RAG Assistant

Spring Boot RAG application for PDF Q&A with sandbox isolation, vector search (pgvector), and OpenAI models.

````
██╗      ██╗   ██╗███╗   ██╗ ██████╗ ██████╗  █████╗ ██╗
██║       ╚██╗ ██╔╝████╗  ██║██╔═══██╗██╔══██╗██╔══██╗██║
██║        ╚████╔╝ ██╔██╗ ██║██║   ██║██████╔╝███████║██║
██║         ╚██╔╝  ██║╚██╗██║██║   ██║██╔══██╗██╔══██║██║
███████╗     ██║   ██║ ╚████║╚██████╔╝██║  ██║██║  ██║██║
╚══════╝     ╚═╝   ╚═╝  ╚═══╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝
````

![Demo Status](https://img.shields.io/badge/Demo-Live-green)

> You can try the live demo version **(v0.0.1)** here: **https://lynorai.space/index.html**
## Current Status

Implemented and aligned in code:
- Unified bootstrap API: one call to initialize app state
- Sandbox lifecycle with remaining time in seconds
- Document readiness recovery after refresh (`documentReady`)
- Streaming answer endpoint (SSE)
- PDF upload + async ingestion + dedup by file hash
- Rate limiting for upload/ask
- Flyway migrations for schema evolution
- Test suite updated to run in Docker-restricted environments by default

## Core Flow

1. Frontend calls `GET /api/bootstrap` (with optional `X-Sandbox-Token`)
2. Backend resolves/creates sandbox and returns:
   - app version/build metadata
   - sandbox token + active flag + remaining seconds
   - `documentReady`
3. Frontend stores `sandbox.token` in localStorage
4. Upload endpoint ingests PDF and builds embeddings/chunks
5. Ask endpoint streams answer tokens via SSE, grounded by retrieved chunks

## API Overview

### `GET /api/bootstrap`
Single initialization endpoint used by frontend.

Response shape:
```json
{
  "version": {
    "version": "0.0.1",
    "commitFull": "abcdef123456...",
    "commitShort": "abcdef1",
    "commitUrl": "https://github.com/devdao2002/enterprise-rag-assistant/commit/abcdef123456...",
    "buildTime": "2026-03-02T00:00:00Z"
  },
  "sandbox": {
    "token": "uuid",
    "active": true,
    "remainingSeconds": 14399
  },
  "documentReady": false
}
```

### `POST /api/documents/upload`
Headers:
- `X-Sandbox-Token: <uuid>`

Body:
- multipart `file` (PDF)

### `GET /api/documents/status/{documentId}`
Headers:
- `X-Sandbox-Token: <uuid>`

### `GET /api/ask/stream?question=...`
Headers:
- `X-Sandbox-Token: <uuid>`

Produces:
- `text/event-stream`

## Tech Stack

- Java 17
- Spring Boot 4.0.3
- Spring AI (OpenAI)
- PostgreSQL + pgvector
- Flyway
- Apache PDFBox
- Bucket4j
- JUnit 5 + Mockito + Testcontainers + WireMock

## Project Structure

```text
src/main/java/com/ducdo/ai_assistant
├── controller
│   ├── AskController.java
│   ├── BootstrapController.java
│   └── DocumentController.java
├── dto/bootstrap
│   ├── BootstrapResponse.java
│   ├── SandboxDto.java
│   └── VersionDto.java
├── model
├── repository
├── security
│   ├── filter
│   └── resolver
├── service
└── util

src/main/resources
├── application.yaml
├── db/migration
└── static/index.html
```

## Configuration

`src/main/resources/application.yaml` expects environment variables:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `OPENAI_API_KEY`
- `OPENAI_CHAT_MODEL` (optional, default `gpt-5-nano`)
- `OPENAI_EMBEDDING_MODEL` (optional, default `text-embedding-3-small`)
- `APP_VERSION` (optional)
- `APP_GIT_COMMIT` (optional)
- `APP_BUILD_TIME` (optional)

## Run Locally (Docker Compose)

1. Set API key:
```bash
export OPENAI_API_KEY=your_key_here
```

2. Start stack:
```bash
docker compose up --build
```

3. Open:
- `http://localhost:8080`

Stop:
```bash
docker compose down
```

## Run Locally (without Docker Compose)

Requirements:
- PostgreSQL with pgvector enabled
- Database/schema accessible by app credentials

Run app:
```bash
./mvnw spring-boot:run
```

## Testing

Default (CI-like local run, no Docker dependency required for pass):
```bash
./mvnw test
```

Notes:
- `DocumentChunkRepositoryTest` uses Testcontainers and is auto-skipped when Docker is unavailable.
- `RagFlowE2ETest` is opt-in and disabled by default.

Run integration E2E explicitly:
```bash
./mvnw -DrunIntegrationTests=true test
```

## Security / Guardrails

- Sandbox token required for protected API routes via `X-Sandbox-Token`
- Sandbox expiry enforced in filter/service
- Upload and ask rate limits enforced by IP
- LLM responses are grounded through retrieved context pipeline

## Progress Next

Planned improvements:
- tighter e2e assertions for SSE payload content
- optional hybrid retrieval (keyword + vector)
- stronger operational metrics/observability
- auth/role model for production multi-organization use
