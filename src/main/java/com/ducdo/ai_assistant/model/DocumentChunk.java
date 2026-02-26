package com.ducdo.ai_assistant.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "token_count")
    private Integer tokenCount;

    /**
     * ⚠️ Không để Hibernate cố map vector khi select
     * Dùng native query để insert và search
     */
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}