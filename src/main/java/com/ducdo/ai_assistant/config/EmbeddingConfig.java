package com.ducdo.ai_assistant.config;

import com.ducdo.ai_assistant.embedding.EmbeddingProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public EmbeddingProvider embeddingProvider(
            @Value("${app.embedding.provider:openai}") String provider,
            @Qualifier("openaiEmbeddingProvider")
            EmbeddingProvider openai,
            @Qualifier("ollamaEmbeddingProvider")
            EmbeddingProvider ollama) {

        if ("ollama".equalsIgnoreCase(provider)) {
            return ollama;
        }

        return openai;
    }
}