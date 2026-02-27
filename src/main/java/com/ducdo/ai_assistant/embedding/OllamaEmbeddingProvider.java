package com.ducdo.ai_assistant.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("ollamaEmbeddingProvider")
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingModel embeddingModel;

    public OllamaEmbeddingProvider(
            @Qualifier("ollamaEmbeddingModel")
            EmbeddingModel embeddingModel) {

        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }
}