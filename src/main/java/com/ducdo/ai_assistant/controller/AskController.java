package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.service.RagService;
import com.ducdo.ai_assistant.service.RateLimitService;
import com.ducdo.ai_assistant.service.RateLimitType;
import com.ducdo.ai_assistant.service.SandboxService;
import com.ducdo.ai_assistant.util.SandboxResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AskController {

    private final RagService ragService;
    private final RateLimitService rateLimitService;
    private final SandboxService sandboxService;
    private final SandboxResolver sandboxResolver;

    public AskController(RagService ragService,
                         RateLimitService rateLimitService,
                         SandboxService sandboxService ,
                         SandboxResolver sandboxResolver) {
        this.ragService = ragService;
        this.rateLimitService=rateLimitService;
        this.sandboxService = sandboxService;
        this.sandboxResolver = sandboxResolver;
    }

    @GetMapping("/ask")
    public ResponseEntity<String> ask(@RequestParam String question,
                      HttpServletRequest request) {
        UUID tenantId = sandboxResolver.resolve(request);

        if (!sandboxService.isValid(tenantId)) {
            return ResponseEntity.badRequest()
                    .body("Sandbox expired.");
        }
        String ip = extractClientIp(request);

        if (!rateLimitService.tryConsume(ip, RateLimitType.ASK)) {
            return ResponseEntity
                    .status(429)
                    .body("Too many requests. Please try again later.");
        }

        String answer = ragService.ask(question, tenantId);
        return ResponseEntity.ok(answer);
    }
    private String extractClientIp(HttpServletRequest request) {

        String header = request.getHeader("X-Forwarded-For");

        if (header != null && !header.isEmpty()) {
            return header.split(",")[0];
        }

        return request.getRemoteAddr();
    }
}