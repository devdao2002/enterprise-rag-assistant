-- =====================================================
-- 1️⃣ Enable pgvector extension
-- =====================================================
CREATE EXTENSION IF NOT EXISTS vector;

-- =====================================================
-- 2️⃣ Tenants Table
-- =====================================================
CREATE TABLE IF NOT EXISTS tenants (
                                       id UUID PRIMARY KEY,
                                       name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- =====================================================
-- 3️⃣ Documents Table
-- =====================================================
CREATE TABLE IF NOT EXISTS documents (
                                         id UUID PRIMARY KEY,
                                         tenant_id UUID NOT NULL,
                                         name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    uploaded_by VARCHAR(100),
    status VARCHAR(50) DEFAULT 'PROCESSED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_documents_tenant
    FOREIGN KEY (tenant_id)
    REFERENCES tenants(id)
    ON DELETE CASCADE
    );

CREATE INDEX idx_documents_tenant
    ON documents(tenant_id);

-- =====================================================
-- 4️⃣ Document Chunks Table
-- =====================================================
CREATE TABLE IF NOT EXISTS document_chunks (
                                               id UUID PRIMARY KEY,
                                               document_id UUID NOT NULL,
                                               tenant_id UUID NOT NULL,
                                               content TEXT NOT NULL,
                                               chunk_index INT,
                                               page_number INT,
                                               token_count INT,
                                               embedding VECTOR(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chunks_document
    FOREIGN KEY (document_id)
    REFERENCES documents(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_chunks_tenant
    FOREIGN KEY (tenant_id)
    REFERENCES tenants(id)
    ON DELETE CASCADE
    );

CREATE INDEX idx_chunks_tenant
    ON document_chunks(tenant_id);

CREATE INDEX idx_chunks_document
    ON document_chunks(document_id);

-- =====================================================
-- 5️⃣ Vector Index (IVFFLAT)
-- IMPORTANT: Run ANALYZE after data inserted
-- =====================================================
CREATE INDEX idx_chunks_embedding
    ON document_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- =====================================================
-- 6️⃣ Query Logs (Audit & Observability)
-- =====================================================
CREATE TABLE IF NOT EXISTS query_logs (
                                          id UUID PRIMARY KEY,
                                          tenant_id UUID,
                                          question TEXT,
                                          response TEXT,
                                          latency_ms INT,
                                          token_usage INT,
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                          CONSTRAINT fk_logs_tenant
                                          FOREIGN KEY (tenant_id)
    REFERENCES tenants(id)
    ON DELETE SET NULL
    );

CREATE INDEX idx_logs_tenant
    ON query_logs(tenant_id);

CREATE INDEX idx_logs_created_at
    ON query_logs(created_at);