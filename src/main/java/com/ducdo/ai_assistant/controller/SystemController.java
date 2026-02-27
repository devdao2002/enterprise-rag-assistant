package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.embedding.EmbeddingProvider;
import com.ducdo.ai_assistant.llm.LlmProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final LlmProvider llmProvider;
    private final EmbeddingProvider embeddingProvider;

    public SystemController(LlmProvider llmProvider,
                            EmbeddingProvider embeddingProvider) {
        this.llmProvider = llmProvider;
        this.embeddingProvider = embeddingProvider;
    }

    @GetMapping("/provider")
    public String provider() {
        return "LLM: " + llmProvider.getClass().getSimpleName()
                + " | Embedding: "
                + embeddingProvider.getClass().getSimpleName();
    }
}