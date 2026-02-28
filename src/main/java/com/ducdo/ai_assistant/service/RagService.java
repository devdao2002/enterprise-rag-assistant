package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.model.QueryLog;
import com.ducdo.ai_assistant.repository.ChunkProjection;
import com.ducdo.ai_assistant.repository.DocumentChunkRepository;
import com.ducdo.ai_assistant.repository.QueryLogRepository;
import com.ducdo.ai_assistant.util.VectorUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
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

    public String ask(String question, UUID tenantId) {

        long startTime = System.currentTimeMillis();

        float[] questionEmbedding = embeddingModel.embed(question);
        String pgVector = VectorUtils.toPgVector(questionEmbedding);

        List<ChunkProjection> chunks =
                chunkRepository.findTopKWithMetadata(tenantId, pgVector, TOP_K);

        if (chunks == null || chunks.isEmpty()) {
            return "I don't have enough information.";
        }

        // ====================================
        // Build Citation Map (deduplicate)
        // Key = documentName + pageNumber
        // ====================================
        record CitationKey(
                String documentName,
                Integer pageNumber
        ) {}

        Map<CitationKey, Integer> citationMap = new LinkedHashMap<>();
        int counter = 1;

        for (ChunkProjection c : chunks) {

            String docName = Optional.ofNullable(c.getDocumentName())
                    .orElse("Unknown Document");

            Integer page = c.getPageNumber();

            CitationKey key = new CitationKey(docName, page);

            if (!citationMap.containsKey(key)) {
                citationMap.put(key, counter++);
            }
        }

        // ====================================
        // Build Context WITH citation index
        // ====================================

        StringBuilder contextBuilder = new StringBuilder();

        for (ChunkProjection c : chunks) {

            String docName = Optional.ofNullable(c.getDocumentName())
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

        // =============================
        // LLM call with instruction
        // =============================

        String answer = chatClient.prompt()
                .system("""
                    You are an internal enterprise knowledge assistant.
                    Only answer using the provided context.
                    Use citation numbers like [1], [2] when referencing information.
                    If not found, say: "I don't have enough information."
                    Provide clear and concise answers.
                    """)
                .user("""
                    Context:
                    %s

                    Question:
                    %s
                    """.formatted(context, question))
                .call()
                .content();

        // =============================
        // Build formatted source list
        // =============================

        List<Map.Entry<CitationKey, Integer>> sorted =
                citationMap.entrySet().stream()
                        .sorted(Comparator
                                .comparing((Map.Entry<CitationKey, Integer> e)
                                        -> e.getKey().documentName())
                                .thenComparing(e ->
                                        Optional.ofNullable(
                                                        e.getKey().pageNumber())
                                                .orElse(0)))
                        .toList();

        String sources = sorted.stream()
                .map(entry -> {

                    CitationKey key = entry.getKey();

                    return "[" + entry.getValue() + "] "
                            + key.documentName()
                            + " (Page "
                            + (key.pageNumber() == null
                            ? "N/A"
                            : key.pageNumber())
                            + ")";
                })
                .collect(Collectors.joining("\n"));

        String finalResponse =
                answer + "\n\nSources:\n" + sources;

        // =============================
        // Logging
        // =============================

        long latency = System.currentTimeMillis() - startTime;

        QueryLog log = new QueryLog();
        log.setId(UUID.randomUUID());
        log.setTenantId(tenantId);
        log.setQuestion(question);
        log.setResponse(answer);
        log.setLatencyMs((int) latency);
        log.setCreatedAt(LocalDateTime.now());

        queryLogRepository.save(log);

        return finalResponse;
    }
}