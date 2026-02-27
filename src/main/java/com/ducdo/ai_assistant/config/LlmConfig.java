package com.ducdo.ai_assistant.config;

import com.ducdo.ai_assistant.llm.LlmProvider;
import com.ducdo.ai_assistant.llm.OllamaProvider;
import com.ducdo.ai_assistant.llm.OpenAiProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

    @Bean
    public LlmProvider llmProvider(
            @Value("${app.llm.provider:openai}") String provider,
            ObjectProvider<OpenAiProvider> openaiProvider,
            ObjectProvider<OllamaProvider> ollamaProvider) {

        if ("ollama".equalsIgnoreCase(provider)) {
            return ollamaProvider.getIfAvailable();
        }

        return openaiProvider.getIfAvailable();
    }
}