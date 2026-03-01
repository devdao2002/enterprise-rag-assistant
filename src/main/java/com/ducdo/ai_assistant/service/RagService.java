package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.model.QueryLog;
import com.ducdo.ai_assistant.repository.ChunkProjection;
import com.ducdo.ai_assistant.repository.DocumentChunkRepository;
import com.ducdo.ai_assistant.repository.QueryLogRepository;
import com.ducdo.ai_assistant.util.VectorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class RagService {

    private final EmbeddingModel embeddingModel;
    private final ChatClient chatClient;
    private final DocumentChunkRepository chunkRepository;
    private final QueryLogRepository queryLogRepository;

    private static final int TOP_K = 5;

    public RagService(EmbeddingModel embeddingModel,
                      ChatClient.Builder builder,
                      DocumentChunkRepository chunkRepository,
                      QueryLogRepository queryLogRepository) {

        this.embeddingModel = embeddingModel;
        this.chatClient = builder.build();
        this.chunkRepository = chunkRepository;
        this.queryLogRepository = queryLogRepository;
    }

    public SseEmitter askStream(String question, UUID tenantId) {

        SseEmitter emitter = new SseEmitter(0L);
        long startTime = System.currentTimeMillis();

        try {

            float[] questionEmbedding = embeddingModel.embed(question);
            String pgVector = VectorUtils.toPgVector(questionEmbedding);

            List<ChunkProjection> chunks =
                    chunkRepository.findTopKWithMetadata(tenantId, pgVector, TOP_K);

            if (chunks == null || chunks.isEmpty()) {

                sendJson(emitter, "token",
                        "I don't have enough information.");
                sendJson(emitter, "done", null);
                emitter.complete();
                return emitter;
            }

            // =============================
            // Build Citation Map
            // =============================

            record CitationKey(String documentName, Integer pageNumber) {}

            Map<CitationKey, Integer> citationMap = new LinkedHashMap<>();
            int counter = 1;

            for (ChunkProjection c : chunks) {

                String docName =
                        Optional.ofNullable(c.getDocumentName())
                                .orElse("Unknown Document");

                Integer page = c.getPageNumber();

                CitationKey key = new CitationKey(docName, page);

                if (!citationMap.containsKey(key)) {
                    citationMap.put(key, counter++);
                }
            }

            // =============================
            // Build Context
            // =============================

            StringBuilder contextBuilder = new StringBuilder();

            for (ChunkProjection c : chunks) {

                String docName =
                        Optional.ofNullable(c.getDocumentName())
                                .orElse("Unknown Document");

                Integer page = c.getPageNumber();

                CitationKey key = new CitationKey(docName, page);

                int index = citationMap.get(key);

                contextBuilder.append("[")
                        .append(index)
                        .append("] ")
                        .append(c.getContent())
                        .append("\n---\n");
            }

            String context = contextBuilder.toString();

            StringBuilder fullAnswer = new StringBuilder();

            // =============================
            // STREAM TOKENS
            // =============================

            chatClient.prompt()
                    .system("""
                        You are an internal enterprise knowledge assistant.
                        Only answer using the provided context.
                        Use citation numbers like [1], [2].
                        If not found, say: "I don't have enough information."
                        """)
                    .user("""
                        Context:
                        %s

                        Question:
                        %s
                        """.formatted(context, question))
                    .stream()
                    .content()
                    .subscribe(

                            token -> {
                                try {
                                    fullAnswer.append(token);
                                    sendJson(emitter, "token", token);
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            },

                            error -> {
                                log.error("Streaming error", error);
                                emitter.completeWithError(error);
                            },

                            () -> {

                                try {

                                    // Send sources separately
                                    for (var entry : citationMap.entrySet()) {

                                        CitationKey key = entry.getKey();

                                        String source =
                                                "[" + entry.getValue() + "] "
                                                        + key.documentName()
                                                        + " (Page "
                                                        + (key.pageNumber() == null
                                                        ? "N/A"
                                                        : key.pageNumber())
                                                        + ")";

                                        sendJson(emitter, "sources", source);
                                    }

                                    sendJson(emitter, "done", null);
                                    emitter.complete();

                                    // Log query
                                    QueryLog logEntity = new QueryLog();
                                    logEntity.setId(UUID.randomUUID());
                                    logEntity.setTenantId(tenantId);
                                    logEntity.setQuestion(question);
                                    logEntity.setResponse(fullAnswer.toString());
                                    logEntity.setLatencyMs(
                                            (int) (System.currentTimeMillis()
                                                    - startTime));
                                    logEntity.setCreatedAt(LocalDateTime.now());

                                    queryLogRepository.save(logEntity);

                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            }
                    );

        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void sendJson(SseEmitter emitter,
                          String type,
                          String content) throws IOException {

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("content", content);

        emitter.send(SseEmitter.event()
                .data(payload));
    }
}