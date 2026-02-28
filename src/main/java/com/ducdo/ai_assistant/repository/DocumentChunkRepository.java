package com.ducdo.ai_assistant.repository;

import com.ducdo.ai_assistant.model.DocumentChunk;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
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
                UUID id,
                UUID documentId,
                UUID tenantId,
                String content,
                Integer chunkIndex,
                Integer pageNumber,
                Integer tokenCount,
                String embedding);

        // ===============================
        // Vector Search (Fast Path)
        // ===============================

        @Query(value = """
        SELECT content
        FROM document_chunks
        WHERE tenant_id = :tenantId
        ORDER BY embedding <-> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
        List<String> findTopKContent(
                UUID tenantId,
                String embedding,
                int limit);

        // ===============================
        // Vector Search with Metadata
        // ===============================

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
                UUID tenantId,
                String embedding,
                int limit);

        // ===============================
        // Delete by document
        // ===============================

        void deleteByDocumentId(UUID documentId);

        // ===============================
        // Delete by tenant (sandbox cleanup)
        // ===============================

        void deleteByTenantId(UUID tenantId);

        // ===============================
        // Count
        // ===============================

        long countByTenantId(UUID tenantId);
}