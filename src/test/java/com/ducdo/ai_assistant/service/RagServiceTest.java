package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.repository.ChunkProjection;
import com.ducdo.ai_assistant.repository.DocumentChunkRepository;
import com.ducdo.ai_assistant.repository.QueryLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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

                SseEmitter emitter = ragService.askStream("What is Java?", tenantId);

                assertThat(emitter).isNotNull();
                // No chunks → no LLM call → no query log saved
                verify(queryLogRepository, never()).save(any());
        }

        @Test
        void ask_chunksFound_shouldCallLlmStreamAndReturnEmitter() {
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

                // Mock the streaming chain:
                // chatClient.prompt().system(...).user(...).stream().content()
                // returns a Flux<String>
                Flux<String> tokenFlux = Flux.just("Java ", "is an ", "OO language.");
                when(chatClient.prompt().system(anyString()).user(anyString()).stream().content())
                                .thenReturn(tokenFlux);

                SseEmitter emitter = ragService.askStream("What is Java?", tenantId);

                assertThat(emitter).isNotNull();
        }
}
