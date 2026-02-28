package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.model.Document;
import com.ducdo.ai_assistant.repository.DocumentChunkRepository;
import com.ducdo.ai_assistant.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IngestionServiceTest {

    private EmbeddingModel embeddingModel;
    private DocumentRepository documentRepository;
    private DocumentChunkRepository chunkRepository;
    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        documentRepository = mock(DocumentRepository.class);
        chunkRepository = mock(DocumentChunkRepository.class);

        ingestionService = new IngestionService(embeddingModel, documentRepository, chunkRepository);
    }

    @Test
    void createDocument_shouldSaveDocumentWithProcessingStatus() {
        UUID tenantId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes());

        UUID docId = ingestionService.createDocument(file, tenantId);

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, times(1)).save(docCaptor.capture());

        Document savedDoc = docCaptor.getValue();
        assertThat(savedDoc.getId()).isEqualTo(docId);
        assertThat(savedDoc.getTenantId()).isEqualTo(tenantId);
        assertThat(savedDoc.getName()).isEqualTo("test.pdf");
        assertThat(savedDoc.getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void ingestPdfAsync_shouldFailOnInvalidPdfAndSetStatus() {
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        byte[] invalidPdfBytes = "invalid pdf data".getBytes();

        ingestionService.ingestPdfAsync(invalidPdfBytes, tenantId, documentId);

        verify(documentRepository, times(1)).updateStatus(documentId, "FAILED");
        verify(chunkRepository, never()).saveAll(any());
    }
}
