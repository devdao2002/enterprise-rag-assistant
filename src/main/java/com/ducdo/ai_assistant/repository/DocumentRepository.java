package com.ducdo.ai_assistant.repository;

import com.ducdo.ai_assistant.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByTenant_Id(UUID tenantId);

    void deleteByTenant_Id(UUID tenantId);
}