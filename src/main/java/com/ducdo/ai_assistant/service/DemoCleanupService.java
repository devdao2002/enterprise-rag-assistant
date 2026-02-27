package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.config.DemoProperties;
import com.ducdo.ai_assistant.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DemoCleanupService {

    private static final Logger log =
            LoggerFactory.getLogger(DemoCleanupService.class);

    private final DocumentRepository repository;
    private final DemoProperties demoProperties;

    public DemoCleanupService(DocumentRepository repository,
                              DemoProperties demoProperties) {
        this.repository = repository;
        this.demoProperties = demoProperties;
    }

    @Scheduled(fixedRateString =
            "#{${app.demo.cleanup-interval-hours} * 3600000}")
    public void cleanup() {

        if (!demoProperties.isAutoCleanupEnabled()) {
            return;
        }

        log.warn("=== DEMO CLEANUP STARTED ===");

        repository.truncateAll();

        log.warn("=== DEMO CLEANUP COMPLETED ===");
    }
}