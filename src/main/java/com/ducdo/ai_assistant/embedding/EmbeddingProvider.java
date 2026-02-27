package com.ducdo.ai_assistant.embedding;

public interface EmbeddingProvider {

    float[] embed(String text);

}