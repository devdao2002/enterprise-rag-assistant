package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.model.QueryLog;
import com.ducdo.ai_assistant.repository.ChunkProjection;
import com.ducdo.ai_assistant.repository.DocumentChunkRepository;
import com.ducdo.ai_assistant.repository.QueryLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RagServiceTest {

    private EmbeddingModel embeddingModel;
    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private DocumentChunkRepository chunkRepository;
    private QueryLogRepository queryLogRepository;
    private RagService ragService;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        chatClientBuilder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        chunkRepository = mock(DocumentChunkRepository.class);
        queryLogRepository = mock(QueryLogRepository.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);

        ragService = new RagService(embeddingModel, chatClientBuilder, chunkRepository, queryLogRepository);
    }

    @Test
    void ask_noChunksFound_shouldReturnFallback() {
        UUID tenantId = UUID.randomUUID();
        when(embeddingModel.embed("What is Java?")).thenReturn(new float[] { 0.1f });
        when(chunkRepository.findTopKWithMetadata(eq(tenantId), anyString(), eq(5)))
                .thenReturn(List.of());

        String response = ragService.ask("What is Java?", tenantId);

        assertThat(response).isEqualTo("I don't have enough information.");
        verify(queryLogRepository, never()).save(any());
    }

    @Test
    void ask_chunksFound_shouldCallLlmAndReturnAnswerWithCitation() {
        UUID tenantId = UUID.randomUUID();
        UUID docId1 = UUID.randomUUID();

        when(embeddingModel.embed("What is Java?")).thenReturn(new float[] { 0.1f });

        ChunkProjection chunk1 = mock(ChunkProjection.class);
        when(chunk1.getContent()).thenReturn("Java is a programming language.");
        when(chunk1.getDocumentId()).thenReturn(docId1);
        when(chunk1.getDocumentName()).thenReturn("intro.pdf");
        when(chunk1.getPageNumber()).thenReturn(1);

        when(chunkRepository.findTopKWithMetadata(eq(tenantId), anyString(), eq(5)))
                .thenReturn(List.of(chunk1));

        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("Java is an OO language.");

        String response = ragService.ask("What is Java?", tenantId);

        assertThat(response).contains("Java is an OO language.");
        assertThat(response).contains("intro.pdf (Page 1)");

        ArgumentCaptor<QueryLog> logCaptor = ArgumentCaptor.forClass(QueryLog.class);
        verify(queryLogRepository).save(logCaptor.capture());

        QueryLog log = logCaptor.getValue();
        assertThat(log.getQuestion()).isEqualTo("What is Java?");
        assertThat(log.getResponse()).isEqualTo("Java is an OO language.");
        assertThat(log.getTenantId()).isEqualTo(tenantId);
    }
}
