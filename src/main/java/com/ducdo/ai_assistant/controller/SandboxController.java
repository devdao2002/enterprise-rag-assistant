package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.service.SandboxService;
import com.ducdo.ai_assistant.util.SandboxResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sandbox")
@RequiredArgsConstructor
public class SandboxController {

    private final SandboxService sandboxService;
    private final SandboxResolver sandboxResolver;

    @PostMapping
    public SandboxResponse createSandbox(
            @RequestHeader(value = "X-Forwarded-For", required = false) String ip
    ) {

        UUID tenantId = UUID.randomUUID();

        sandboxService.createSandbox(
                tenantId,
                ip != null ? ip : "unknown"
        );

        var sandbox = sandboxService.getSandbox(tenantId);

        long remainingMinutes = Duration
                .between(LocalDateTime.now(), sandbox.getExpiresAt())
                .toMinutes();

        return new SandboxResponse(
                tenantId,
                sandbox.getExpiresAt(),
                remainingMinutes
        );
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(HttpServletRequest request) {

        UUID tenantId = sandboxResolver.resolve(request);

        if (!sandboxService.isValid(tenantId)) {
            return ResponseEntity.status(400).build();
        }

        long minutes = sandboxService.remainingMinutes(tenantId);

        return ResponseEntity.ok(Map.of(
                "remainingMinutes", minutes
        ));
    }

    public record SandboxResponse(
            UUID sandboxToken,
            LocalDateTime expiresAt,
            long remainingMinutes
    ) {}
}