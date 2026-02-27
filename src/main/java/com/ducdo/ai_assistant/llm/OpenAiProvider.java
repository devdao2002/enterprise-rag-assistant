package com.ducdo.ai_assistant.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("openaiProvider")
public class OpenAiProvider implements LlmProvider {

    private final ChatClient chatClient;
    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);

    public OpenAiProvider(
            @Qualifier("openAiChatModel")
            org.springframework.ai.chat.model.ChatModel chatModel) {

        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        log.info("LLM Provider -> OpenAI");
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
}