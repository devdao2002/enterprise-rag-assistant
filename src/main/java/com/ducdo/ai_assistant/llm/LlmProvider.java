package com.ducdo.ai_assistant.llm;

public interface LlmProvider {

    String chat(String systemPrompt, String userPrompt);
}