CREATE TABLE sandbox_sessions (
                                  tenant_id UUID PRIMARY KEY,
                                  created_at TIMESTAMP NOT NULL,
                                  expires_at TIMESTAMP NOT NULL,
                                  ip_address TEXT
);

ALTER TABLE documents
    ADD CONSTRAINT fk_documents_tenant_session
        FOREIGN KEY (tenant_id)
            REFERENCES sandbox_sessions(tenant_id)
            ON DELETE CASCADE;