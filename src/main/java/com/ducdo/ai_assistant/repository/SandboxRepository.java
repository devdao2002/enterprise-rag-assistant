package com.ducdo.ai_assistant.repository;

import com.ducdo.ai_assistant.model.SandboxSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SandboxRepository
        extends JpaRepository<SandboxSession, UUID> {

    @Query("""
        SELECT s.tenantId
        FROM SandboxSession s
        WHERE s.expiresAt < :now
    """)
    List<UUID> findExpiredTenantIds(LocalDateTime now);
}