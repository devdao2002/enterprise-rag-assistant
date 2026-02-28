package com.ducdo.ai_assistant.repository;

import com.ducdo.ai_assistant.model.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QueryLogRepository extends JpaRepository<QueryLog, UUID> {

    List<QueryLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    long countByTenantId(UUID tenantId);
}