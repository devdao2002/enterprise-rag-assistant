package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.dto.bootstrap.BootstrapResponse;
import com.ducdo.ai_assistant.dto.bootstrap.SandboxDto;
import com.ducdo.ai_assistant.dto.bootstrap.VersionDto;
import com.ducdo.ai_assistant.security.resolver.SandboxResolver;

import com.ducdo.ai_assistant.service.IngestionService;
import com.ducdo.ai_assistant.service.SandboxService;

import com.ducdo.ai_assistant.service.VersionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/bootstrap")
@RequiredArgsConstructor
public class BootstrapController {

    private final SandboxService sandboxService;
    private final SandboxResolver sandboxResolver;
    private final IngestionService ingestionService;
    private final VersionService versionService;

    @GetMapping
    public BootstrapResponse bootstrap(HttpServletRequest request) {
        UUID tenantId = resolveOrCreateSandbox(request);
        var sandbox = sandboxService.getSandbox(tenantId);
        long remainingSeconds =
                Duration.between(
                        LocalDateTime.now(),
                        sandbox.getExpiresAt()
                ).getSeconds();

        boolean hasDocument =
                ingestionService.hasReadyDocument(tenantId);


        return BootstrapResponse.builder()
                .version(
                        versionService.getVersionInfo()
                )
                .sandbox(
                        new SandboxDto(
                                tenantId,
                                true,
                                remainingSeconds
                        )
                )
                .documentReady(hasDocument)
                .build();
    }

    private UUID resolveOrCreateSandbox(
            HttpServletRequest request
    ) {
        try {
            UUID tenantId =
                    sandboxResolver.resolve(request);
            if (sandboxService.isValid(tenantId)) {
                return tenantId;
            }
        } catch (Exception ignored) {}

        // Create new sandbox if invalid
        UUID newTenant = UUID.randomUUID();
        String ip =
                request.getHeader("X-Forwarded-For");
        sandboxService.createSandbox(
                newTenant,
                ip != null ? ip : request.getRemoteAddr()
        );
        return newTenant;
    }
}