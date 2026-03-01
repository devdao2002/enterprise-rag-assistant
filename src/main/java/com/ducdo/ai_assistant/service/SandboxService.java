package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.model.SandboxSession;
import com.ducdo.ai_assistant.repository.SandboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SandboxService {

    private static final Logger log =
            LoggerFactory.getLogger(SandboxService.class);

    private final SandboxRepository repository;

    public SandboxService(SandboxRepository repository) {
        this.repository = repository;
    }

    // =============================
    // CREATE SANDBOX
    // =============================

    @Transactional
    public void createSandbox(UUID tenantId, String ipAddress) {

        SandboxSession sandbox = SandboxSession.builder()
                .tenantId(tenantId)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(4))
                .ipAddress(ipAddress)
                .build();

        repository.save(sandbox);

        log.info("Created sandbox tenantId={} expiresAt={}",
                tenantId,
                sandbox.getExpiresAt());
    }

    // =============================
    // CHECK VALID
    // =============================

    public boolean isValid(UUID tenantId) {

        return repository.findById(tenantId)
                .map(s -> !s.isExpired())
                .orElse(false);
    }

    // =============================
    // CLEANUP JOB
    // =============================

    @Transactional
    @Scheduled(fixedRate = 600000) // 10 mins
    public void cleanupExpiredSandboxes() {

        List<UUID> expired =
                repository.findExpiredTenantIds(LocalDateTime.now());

        if (expired.isEmpty()) {
            return;
        }

        for (UUID tenantId : expired) {
            repository.deleteById(tenantId);
            log.warn("Deleted expired sandbox tenantId={}", tenantId);
        }
    }

    // =============================
    // REMAINING TIME
    // =============================

    public long remainingMinutes(UUID tenantId) {

        return repository.findById(tenantId)
                .map(sandbox -> {

                    if (sandbox.isExpired()) {
                        return 0L;
                    }

                    return java.time.Duration
                            .between(LocalDateTime.now(), sandbox.getExpiresAt())
                            .toMinutes();

                })
                .orElse(0L);
    }

    public SandboxSession getSandbox(UUID tenantId) {
        return repository.findById(tenantId).orElse(null);
    }
}