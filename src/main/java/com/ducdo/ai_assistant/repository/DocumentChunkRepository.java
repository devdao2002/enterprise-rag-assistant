package com.ducdo.ai_assistant.repository;

import com.ducdo.ai_assistant.model.DocumentChunk;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, UUID> {

    // ===============================
    // Native insert (vector-safe)
    // ===============================

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO document_chunks
        (id, document_id, tenant_id, content, chunk_index,
         page_number, token_count, embedding, created_at)
        VALUES
        (:id, :documentId, :tenantId, :content, :chunkIndex,
         :pageNumber, :tokenCount,
         CAST(:embedding AS vector),
         CURRENT_TIMESTAMP)
        """, nativeQuery = true)
    void insertChunk(
            @Param("id") UUID id,
            @Param("documentId") UUID documentId,
            @Param("tenantId") UUID tenantId,
            @Param("content") String content,
            @Param("chunkIndex") Integer chunkIndex,
            @Param("pageNumber") Integer pageNumber,
            @Param("tokenCount") Integer tokenCount,
            @Param("embedding") String embedding
    );

    // =====================================
    // Semantic similarity search (RAG)
    // =====================================

    @Query(value = """
        SELECT content
        FROM document_chunks
        WHERE tenant_id = :tenantId
        ORDER BY embedding <-> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<String> findTopKContent(
            @Param("tenantId") UUID tenantId,
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );

    // =====================================
    // Similarity search with metadata
    // =====================================

    @Query(value = """
    SELECT content AS content,
           document_id AS documentId,
           page_number AS pageNumber
    FROM document_chunks
    WHERE tenant_id = :tenantId
    ORDER BY embedding <-> CAST(:embedding AS vector)
    LIMIT :limit
    """, nativeQuery = true)
    List<ChunkProjection> findTopKWithMetadata(
            @Param("tenantId") UUID tenantId,
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );

    // =====================================
    // Delete by document
    // =====================================

    @Modifying
    @Transactional
    void deleteByDocument_Id(UUID documentId);

    // =====================================
    // Count chunks per tenant
    // =====================================

    long countByTenantId(UUID tenantId);
}