-- ===============================================
-- V3 - Remove legacy tenants and migrate to sandbox_sessions
-- ===============================================

-- 1️⃣ Drop foreign keys referencing tenants

ALTER TABLE document_chunks
DROP CONSTRAINT IF EXISTS fk_chunks_tenant;

ALTER TABLE documents
DROP CONSTRAINT IF EXISTS fk_documents_tenant;

ALTER TABLE query_logs
DROP CONSTRAINT IF EXISTS fk_logs_tenant;

-- 2️⃣ Drop tenants table (ONLY if demo sandbox system)

DROP TABLE IF EXISTS tenants CASCADE;

-- 3️⃣ Ensure documents reference sandbox_sessions

ALTER TABLE documents
DROP CONSTRAINT IF EXISTS fk_documents_tenant_session;

ALTER TABLE documents
    ADD CONSTRAINT fk_documents_sandbox
        FOREIGN KEY (tenant_id)
            REFERENCES sandbox_sessions(tenant_id)
            ON DELETE CASCADE;

-- 4️⃣ Ensure chunks reference sandbox_sessions

ALTER TABLE document_chunks
DROP CONSTRAINT IF EXISTS fk_chunks_sandbox;

ALTER TABLE document_chunks
    ADD CONSTRAINT fk_chunks_sandbox
        FOREIGN KEY (tenant_id)
            REFERENCES sandbox_sessions(tenant_id)
            ON DELETE CASCADE;

-- 5️⃣ Update query_logs to reference sandbox

ALTER TABLE query_logs
    ADD CONSTRAINT fk_logs_sandbox
        FOREIGN KEY (tenant_id)
            REFERENCES sandbox_sessions(tenant_id)
            ON DELETE SET NULL;

-- ===============================================
-- END V3
-- ===============================================