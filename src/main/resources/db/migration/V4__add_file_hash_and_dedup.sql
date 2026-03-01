-- =====================================================
-- V4: Add file hash for duplicate prevention
-- =====================================================

-- Add file_hash column (nullable first for safe rollout)

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS file_hash VARCHAR(64);


-- Backfill existing rows (optional safety)
-- If you previously uploaded documents, they will remain NULL.
-- You may leave them NULL or handle manually later.


-- Create index for fast lookup (tenant scoped)

CREATE INDEX IF NOT EXISTS idx_documents_tenant_hash
    ON documents(tenant_id, file_hash);


-- Enforce uniqueness per tenant (only when hash is NOT NULL)

-- We use partial unique index to avoid conflicts with old NULL rows
CREATE UNIQUE INDEX IF NOT EXISTS uq_documents_tenant_hash
    ON documents(tenant_id, file_hash)
    WHERE file_hash IS NOT NULL;


-- ptional: Add NOT NULL in future (after rollout)
-- DO NOT enable immediately in production if old rows exist
-- ALTER TABLE documents ALTER COLUMN file_hash SET NOT NULL;