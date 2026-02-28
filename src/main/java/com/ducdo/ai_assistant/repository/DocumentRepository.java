package com.ducdo.ai_assistant.repository;

import com.ducdo.ai_assistant.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByTenantId(UUID tenantId);

    void deleteByTenantId(UUID tenantId);

    @Modifying
    @Transactional
    @Query(
            value = "TRUNCATE TABLE document_chunks, documents RESTART IDENTITY CASCADE",
            nativeQuery = true
    )
    void truncateAll();

    @Modifying
    @Query("""
    UPDATE Document d
    SET d.status = :status
    WHERE d.id = :documentId
""")
    void updateStatus(
            @Param("documentId") UUID documentId,
            @Param("status") String status
    );
}