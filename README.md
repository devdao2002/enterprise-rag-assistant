# Enterprise Internal AI Document Q&A (RAG)
An enterprise-grade internal AI assistant built with Spring Boot, PostgreSQL + pgvector, and OpenAI, implementing Retrieval-Augmented Generation (RAG) with citation support.

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

**Features**
- Upload PDF documents

- Smart chunking with overlap

- OpenAI embeddings (text-embedding-3-small)

- Semantic similarity search via pgvector

- Guarded LLM response (no hallucination outside context)

- Citation with document + page reference

- Multi-tenant architecture

- Query logging & latency tracking

- Flyway database migration

**Architecture**

````
                         ┌──────────────────────┐
                         │        USER          │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │   Spring Boot API    │
                         │  (RAG Orchestration) │
                         └──────────┬───────────┘
                                    │
               ┌────────────────────┼────────────────────┐
               ▼                    ▼                    ▼
      ┌────────────────┐  ┌────────────────┐  ┌────────────────┐
      │ OpenAI         │  │ PostgreSQL     │  │ Flyway         │
      │ Embeddings     │  │ + pgvector     │  │ Migration      │
      └────────────────┘  └────────────────┘  └────────────────┘
               │                    │
               ▼                    ▼
         ┌─────────────────────────────────────┐
         │  Semantic Search (Top-K Retrieval)  │
         └─────────────────────────────────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │  OpenAI Chat Model   │
                         │  (Guarded Response)  │
                         └──────────┬───────────┘
                                    │
                                    ▼
                     ┌────────────────────────────┐
                     │  Answer + Citation Output  │
                     └────────────────────────────┘
````

**Project Structure**

````
ai-assistant/
 ├── controller/
 ├── service/
 │     ├── RagService
 │     ├── IngestionService
 ├── repository/
 ├── model/
 ├── util/
 ├── config/
 └── db/migration/
````

**Tech Stack** :
Java 17 |
Spring Boot |
Spring AI |
PostgreSQL |
pgvector |
Flyway |
Apache PDFBox |
OpenAI API

**Setup**

1/ Start PostgreSQL + pgvector (Docker) 
````
docker run -d \
  --name pgvector \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  ankane/pgvector
````
2/ Create database
````
bash
docker exec -it pgvector psql -U postgres

sql
CREATE DATABASE ai;
````

3/ Configure application.yml

````
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai
    username: postgres
    password: password

  flyway:
    enabled: true
    baseline-on-migrate: true

  jpa:
    hibernate:
      ddl-auto: none

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-5-nano
      embedding:
        options:
          model: text-embedding-3-small
````

4/ Set OpenAI API key
````
export OPENAI_API_KEY=your_api_key_here
````

5/ Run application
````
bash
./mvnw spring-boot:run
````
Flyway will automatically create schema.

**Create Tenant (Required Before Upload)**
````
INSERT INTO tenants (id, name)
VALUES ('11111111-1111-1111-1111-111111111111', 'CompanyA');
````

**Upload PDF**

````
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@./sample.pdf" \
  -F "tenantId=11111111-1111-1111-1111-111111111111"
````

**Ask Question**
````
curl -G http://localhost:8080/api/ask \
  --data-urlencode "question=How many leave days do employees have?" \
  --data-urlencode "tenantId=11111111-1111-1111-1111-111111111111"
````

**RAG Flow**
1.	Extract text from PDF
2.	Chunk with overlap (1000 chars / 200 overlap)
3.	Generate embedding
4.	Store in pgvector
5.	Embed user question
6.	Retrieve top-K similar chunks
7.	Inject context into LLM
8.	Return answer + citation

**Database Schema**
- tenants

- documents

- document_chunks (vector indexed)

- query_logs

**Guardrail Prompt**

The system strictly answers only using retrieved context:

>“Only answer using the provided context. If not found, say you don’t know.”

Prevents hallucination.

**Example Response**
````
Employees have 15 days annual leave.

Source: DocumentId=480b78ec..., Page=3
````

## Run with Docker (Recommended)
This project is fully containerized using Docker + Docker Compose.

It will start:
- Spring Boot RAG Application
- PostgreSQL with pgvector
- Automatic Flyway migration
- Persistent database volume

**Prerequisites**
- Docker installed
- Docker Compose installed 
- OpenAI API Key

Check Docker:

````
docker --version
docker compose version
````

**Set OpenAI API Key**

Export your API key:
````
export OPENAI_API_KEY=your_api_key_here
````
Or create a .env file:
````
OPENAI_API_KEY=your_api_key_here
````
Docker Compose will automatically load it.

**Start Full Stack**
````
docker compose up --build
````
**Access Application**
````
http://localhost:8080
````
**Create Tenant (One-time setup)**

Connect to Postgres:
````
docker exec -it rag_postgres psql -U postgres -d ai
````

Create tenant:
````
INSERT INTO tenants (id, name)
VALUES ('11111111-1111-1111-1111-111111111111', 'CompanyA');
````

**Upload PDF**

````
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@./sample.pdf" \
  -F "tenantId=11111111-1111-1111-1111-111111111111"
````

**Ask Question**

````
curl -G http://localhost:8080/api/ask \
  --data-urlencode "question=How many leave days do employees have?" \
  --data-urlencode "tenantId=11111111-1111-1111-1111-111111111111"
````

Expected response:

````
Employees have 15 days annual leave.

Source: DocumentId=xxxx-xxxx, Page=3
````

**Stop Containers**
````
docker compose down
````

**Stop and Remove Database Volume**

*This deletes all stored documents and embeddings.*

````
docker compose down -v
````

**Inspect Database**

````
docker exec -it rag_postgres psql -U postgres -d ai
````






**Production Enhancements (Future)**
- WT authentication
- Role-based document access
- Async ingestion
- Streaming responses
- Hybrid search (BM25 + vector)
- Redis caching
- HNSW vector index
- Admin dashboard
- SaaS multi-organization support

**Why This Matters**

This project demonstrates:

- Enterprise-ready RAG architecture
- Multi-tenant AI backend
- Vector search engineering
- LLM guardrail implementation
- Clean layered Spring architecture

