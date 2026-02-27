package com.ducdo.ai_assistant.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("openaiEmbeddingProvider")
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingModel embeddingModel;

    public OpenAiEmbeddingProvider(
            @Qualifier("openAiEmbeddingModel")
            EmbeddingModel embeddingModel) {

        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }
}