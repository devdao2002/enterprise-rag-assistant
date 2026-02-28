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

        // ============================================
        // Native Insert (Vector Safe)
        // ============================================

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
                        @Param("embedding") String embedding);

        // ============================================
        // Vector Search (Content Only)
        // ============================================

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
                        @Param("limit") int limit);

        // ============================================
        // Vector Search with Metadata (JOIN documents)
        // ============================================

        @Query(value = """
                        SELECT dc.content       AS content,
                               dc.document_id   AS documentId,
                               dc.page_number   AS pageNumber,
                               d.name           AS documentName
                        FROM document_chunks dc
                        JOIN documents d ON dc.document_id = d.id
                        WHERE dc.tenant_id = :tenantId
                        ORDER BY dc.embedding <-> CAST(:embedding AS vector)
                        LIMIT :limit
                        """, nativeQuery = true)
        List<ChunkProjection> findTopKWithMetadata(
                        @Param("tenantId") UUID tenantId,
                        @Param("embedding") String embedding,
                        @Param("limit") int limit);

        // ============================================
        // Delete by Document
        // ============================================

        @Modifying
        @Transactional
        @Query("DELETE FROM DocumentChunk c WHERE c.documentId = :documentId")
        void deleteByDocumentId(@Param("documentId") UUID documentId);

        // ============================================
        // Delete by Tenant (Sandbox Cleanup)
        // ============================================

        @Modifying
        @Transactional
        @Query("DELETE FROM DocumentChunk c WHERE c.tenantId = :tenantId")
        void deleteByTenantId(@Param("tenantId") UUID tenantId);

        // ============================================
        // Count
        // ============================================

        long countByTenantId(UUID tenantId);
}