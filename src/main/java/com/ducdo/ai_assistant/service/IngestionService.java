package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.embedding.EmbeddingProvider;
import com.ducdo.ai_assistant.model.Document;
import com.ducdo.ai_assistant.model.Tenant;
import com.ducdo.ai_assistant.repository.DocumentChunkRepository;
import com.ducdo.ai_assistant.repository.DocumentRepository;
import com.ducdo.ai_assistant.repository.TenantRepository;
import com.ducdo.ai_assistant.util.TextChunker;
import com.ducdo.ai_assistant.util.VectorUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class IngestionService {

    private final EmbeddingProvider embeddingProvider;
    private final TenantRepository tenantRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;

    public IngestionService(EmbeddingProvider embeddingProvider,
                            TenantRepository tenantRepository,
                            DocumentRepository documentRepository,
                            DocumentChunkRepository chunkRepository) {

        this.embeddingProvider = embeddingProvider;
        this.tenantRepository = tenantRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    public void ingestPdf(MultipartFile file, UUID tenantId) throws Exception {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // 1️⃣ Create document record
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setTenant(tenant);
        document.setName(file.getOriginalFilename());
        document.setFileType("PDF");
        document.setFileSize(file.getSize());
        document.setStatus("PROCESSING");

        documentRepository.save(document);

        // 2️⃣ Extract text from PDF
        try (InputStream is = file.getInputStream();
             PDDocument pdf = PDDocument.load(is)) {

            PDFTextStripper stripper = new PDFTextStripper();

            int totalPages = pdf.getNumberOfPages();

            for (int page = 1; page <= totalPages; page++) {

                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String pageText = stripper.getText(pdf);

                // 3️⃣ Chunk with overlap
                List<String> chunks =
                        TextChunker.chunkText(pageText, 1000, 200);

                int chunkIndex = 0;

                for (String chunk : chunks) {

                    // 4️⃣ Generate embedding
                    float[] embedding = embeddingProvider.embed(chunk);
                    String pgVector = VectorUtils.toPgVector(embedding);

                    // 5️⃣ Native insert
                    chunkRepository.insertChunk(
                            UUID.randomUUID(),
                            document.getId(),
                            tenantId,
                            chunk,
                            chunkIndex,
                            page,
                            chunk.length(),
                            pgVector
                    );

                    chunkIndex++;
                }
            }
        }

        document.setStatus("PROCESSED");
        documentRepository.save(document);
    }
}