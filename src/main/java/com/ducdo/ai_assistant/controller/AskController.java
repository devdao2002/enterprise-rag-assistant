package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.service.RagService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AskController {

    private final RagService ragService;

    public AskController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question,
                      @RequestParam UUID tenantId) {

        return ragService.ask(question, tenantId);
    }
}