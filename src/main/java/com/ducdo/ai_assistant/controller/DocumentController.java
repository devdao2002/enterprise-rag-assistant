package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.model.Document;
import com.ducdo.ai_assistant.repository.DocumentRepository;
import com.ducdo.ai_assistant.service.IngestionService;
import com.ducdo.ai_assistant.util.HashUtils;
import com.ducdo.ai_assistant.security.resolver.SandboxResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final IngestionService ingestionService;
    private final SandboxResolver sandboxResolver;
    private final DocumentRepository documentRepository;

    public DocumentController(IngestionService ingestionService,
                              SandboxResolver sandboxResolver,
                              DocumentRepository documentRepository) {
        this.ingestionService = ingestionService;
        this.sandboxResolver = sandboxResolver;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) throws Exception {
        UUID tenantId = sandboxResolver.resolve(request);
        byte[] fileBytes = file.getBytes();

        if (fileBytes.length < 4 ||
                fileBytes[0] != '%' ||
                fileBytes[1] != 'P' ||
                fileBytes[2] != 'D' ||
                fileBytes[3] != 'F') {

            return ResponseEntity
                    .status(429)
                    .body(Map.of(
                            "status", "ERROR",
                            "code", 429,
                            "message", "Invalid PDF file."
            ));
        }



        String hash = HashUtils.sha256(fileBytes);

        if (documentRepository.existsByTenantIdAndFileHash(tenantId, hash)) {
            return ResponseEntity
                    .status(429).body(Map.of(
                            "status", "ERROR",
                            "code", 429,
                            "message","This document has already been uploaded."
                    ));
        }

        UUID documentId =
                ingestionService.createDocument(file, tenantId,hash);

        ingestionService.ingestPdfAsync(
                fileBytes,
                tenantId,
                documentId
        );

        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "status", "PROCESSING"
        ));
    }

    @GetMapping("/status/{documentId}")
    public ResponseEntity<?> getStatus(
            @PathVariable UUID documentId,
            HttpServletRequest request) {

        UUID tenantId = sandboxResolver.resolve(request);

        Document doc = documentRepository
                .findById(documentId)
                .orElse(null);

        if (doc == null ||
                !doc.getTenantId().equals(tenantId)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "status", doc.getStatus()
        ));
    }

    @GetMapping("/exists")
    public ResponseEntity<?> hasDocument(HttpServletRequest request) {

        UUID tenantId = sandboxResolver.resolve(request);

        boolean exists =
                documentRepository.existsByTenantIdAndStatus(
                        tenantId,
                        "READY"
                );

        return ResponseEntity.ok(Map.of(
                "hasDocument", exists
        ));
    }
}