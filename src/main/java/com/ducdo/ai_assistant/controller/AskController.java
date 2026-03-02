package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.service.RagService;
import com.ducdo.ai_assistant.service.SandboxService;
import com.ducdo.ai_assistant.security.resolver.SandboxResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AskController {

    private final RagService ragService;
    private final SandboxService sandboxService;
    private final SandboxResolver sandboxResolver;

    public AskController(RagService ragService,
                         SandboxService sandboxService ,
                         SandboxResolver sandboxResolver) {
        this.ragService = ragService;
        this.sandboxService = sandboxService;
        this.sandboxResolver = sandboxResolver;
    }

    @GetMapping(value = "/ask/stream", produces = "text/event-stream")
    public SseEmitter ask(@RequestParam String question,
                          HttpServletRequest request) {

        SseEmitter emitter = new SseEmitter(0L);

        try {

            UUID tenantId = sandboxResolver.resolve(request);

            if (!sandboxService.isValid(tenantId)) {
                emitter.send("Sandbox expired.");
                emitter.complete();
                return emitter;
            }

            return ragService.askStream(question, tenantId);

        } catch (Exception e) {
            try {
                emitter.send("Internal error.");
            } catch (Exception ignored) {}
            emitter.completeWithError(e);
            return emitter;
        }
    }
}