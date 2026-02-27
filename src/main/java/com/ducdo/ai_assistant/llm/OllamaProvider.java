package com.ducdo.ai_assistant.llm;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("ollamaProvider")
public class OllamaProvider implements LlmProvider {

    private final ChatClient chatClient;
    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);

    public OllamaProvider(
            @Qualifier("ollamaChatModel")
            org.springframework.ai.chat.model.ChatModel chatModel) {

        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        log.info("LLM Provider -> Ollama");
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
}