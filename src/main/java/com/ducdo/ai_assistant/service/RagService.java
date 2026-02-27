package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.embedding.EmbeddingProvider;
import com.ducdo.ai_assistant.llm.LlmProvider;
import com.ducdo.ai_assistant.model.QueryLog;
import com.ducdo.ai_assistant.repository.ChunkProjection;
import com.ducdo.ai_assistant.repository.DocumentChunkRepository;
import com.ducdo.ai_assistant.repository.QueryLogRepository;
import com.ducdo.ai_assistant.util.VectorUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final EmbeddingProvider embeddingProvider;
    private final LlmProvider llmProvider;
    private final DocumentChunkRepository chunkRepository;
    private final QueryLogRepository queryLogRepository;

    public RagService(EmbeddingProvider embeddingProvider,
                      LlmProvider llmProvider,
                      DocumentChunkRepository chunkRepository,
                      QueryLogRepository queryLogRepository) {

        this.embeddingProvider = embeddingProvider;
        this.llmProvider = llmProvider;
        this.chunkRepository = chunkRepository;
        this.queryLogRepository = queryLogRepository;
    }

    public String ask(String question, UUID tenantId) {

        long startTime = System.currentTimeMillis();

        // 1️⃣ Embed question
        float[] questionEmbedding = embeddingProvider.embed(question);
        String pgVector = VectorUtils.toPgVector(questionEmbedding);

        // 2️⃣ Retrieve top K similar chunks
        List<ChunkProjection> chunks =
                chunkRepository.findTopKWithMetadata(tenantId, pgVector, 5);

        if (chunks == null || chunks.isEmpty()) {
            return "I don't have enough information.";
        }

        // 3️⃣ Build context
        String context = chunks.stream()
                .map(ChunkProjection::getContent)
                .collect(Collectors.joining("\n---\n"));

        // 4️⃣ Call LLM
        String systemPrompt = """
        You are an internal enterprise knowledge assistant.
        Only answer using the provided context.
        If the answer is not explicitly in the context,
        respond with: "I don't have enough information."
        Provide clear and concise answers.
        """;

        String userPrompt = """
        Context:
        %s

        Question:
        %s
        """.formatted(context, question);

        String answer = llmProvider.chat(systemPrompt, userPrompt);

        // 5️⃣ Build citation
        String citation = chunks.stream()
                .map(c -> "Source: DocumentId=" + c.getDocumentId()
                        + ", Page=" + c.getPageNumber())
                .distinct()
                .collect(Collectors.joining("\n"));

        String finalResponse = answer + "\n\n" + citation;

        // 6️⃣ Log query
        long latency = System.currentTimeMillis() - startTime;

        QueryLog log = new QueryLog();
        log.setId(UUID.randomUUID());
        log.setQuestion(question);
        log.setResponse(answer);
        log.setLatencyMs((int) latency);
        log.setCreatedAt(LocalDateTime.now());

        queryLogRepository.save(log);

        return finalResponse;
    }
}