package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.model.Document;
import com.ducdo.ai_assistant.repository.DocumentChunkRepository;
import com.ducdo.ai_assistant.repository.DocumentRepository;
import com.ducdo.ai_assistant.util.TextChunker;
import com.ducdo.ai_assistant.model.DocumentChunk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final EmbeddingModel embeddingModel;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;
    private static final int EMBEDDING_BATCH_SIZE = 50;

    /**
     * Create document record and return immediately
     */
    public UUID createDocument(MultipartFile file, UUID tenantId,String hash) {

        Document document = Document.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name(file.getOriginalFilename())
                .fileType("PDF")
                .fileSize(file.getSize())
                .status("PROCESSING")
                .createdAt(LocalDateTime.now())
                .build();
        document.setFileHash(hash);

        documentRepository.save(document);

        return document.getId();
    }

    /**
     * Async ingestion (non-blocking)
     */
    @Async("ingestionExecutor")
    @Transactional
    public void ingestPdfAsync(byte[] fileBytes,
                               UUID tenantId,
                               UUID documentId) {

        long start = System.currentTimeMillis();

        try (PDDocument pdf =
                     PDDocument.load(new ByteArrayInputStream(fileBytes))) {

            PDFTextStripper stripper = new PDFTextStripper();

            record ChunkData(String content, int pageNumber) {}

            int totalPages = pdf.getNumberOfPages();

            List<ChunkData> allChunks = new ArrayList<>();

            for (int page = 1; page <= totalPages; page++) {

                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String pageText = stripper.getText(pdf);

                List<String> chunks =
                        TextChunker.chunkText(pageText,
                                CHUNK_SIZE,
                                CHUNK_OVERLAP);

                for (String chunk : chunks) {
                    allChunks.add(new ChunkData(chunk, page));
                }
            }

            log.info("Total chunks generated: {}", allChunks.size());

            // =========================
            // Batch Embedding + Insert
            // =========================
            for (int i = 0; i < allChunks.size(); i += EMBEDDING_BATCH_SIZE) {

                int end = Math.min(i + EMBEDDING_BATCH_SIZE, allChunks.size());

                List<ChunkData> batch = allChunks.subList(i, end);

                List<String> texts = batch.stream()
                        .map(ChunkData::content)
                        .toList();

                List<float[]> embeddings = embeddingModel.embed(texts);

                List<DocumentChunk> entities = new ArrayList<>();

                for (int j = 0; j < batch.size(); j++) {

                    ChunkData chunkData = batch.get(j);

                    entities.add(
                            DocumentChunk.builder()
                                    .id(UUID.randomUUID())
                                    .documentId(documentId)
                                    .tenantId(tenantId)
                                    .content(chunkData.content())
                                    .chunkIndex(i + j)
                                    .pageNumber(chunkData.pageNumber())   // 🔥 FIX
                                    .tokenCount(chunkData.content().length())
                                    .embedding(embeddings.get(j))
                                    .createdAt(LocalDateTime.now())
                                    .build()
                    );
                }

                chunkRepository.saveAll(entities);

                log.info("Inserted batch {} - {}", i, end);
            }

            documentRepository.updateStatus(documentId, "READY");

            long duration = System.currentTimeMillis() - start;

            log.info("Ingestion completed in {} ms", duration);

        } catch (Exception e) {

            log.error("Ingestion failed", e);
            documentRepository.updateStatus(documentId, "FAILED");
        }
    }
}