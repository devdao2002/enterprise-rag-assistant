package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.service.RagService;
import com.ducdo.ai_assistant.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AskController {

    private final RagService ragService;
    private final RateLimitService rateLimitService;

    public AskController(RagService ragService,
                         RateLimitService rateLimitService) {
        this.ragService = ragService;
        this.rateLimitService=rateLimitService;
    }

    @GetMapping("/ask")
    public ResponseEntity<String> ask(@RequestParam String question,
                      @RequestParam UUID tenantId,
                      HttpServletRequest request) {
        String ip = extractClientIp(request);

        if (!rateLimitService.tryConsume(ip)) {
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