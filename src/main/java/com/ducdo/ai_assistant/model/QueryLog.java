package com.ducdo.ai_assistant.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "query_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryLog {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}