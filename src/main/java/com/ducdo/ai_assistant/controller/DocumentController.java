package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.model.Document;
import com.ducdo.ai_assistant.repository.DocumentRepository;
import com.ducdo.ai_assistant.service.IngestionService;
import com.ducdo.ai_assistant.service.RateLimitService;
import com.ducdo.ai_assistant.service.RateLimitType;
import com.ducdo.ai_assistant.service.SandboxService;
import com.ducdo.ai_assistant.util.HashUtils;
import com.ducdo.ai_assistant.util.SandboxResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final IngestionService ingestionService;
    private final RateLimitService rateLimitService;
    private final SandboxService sandboxService;
    private final SandboxResolver sandboxResolver;
    private final DocumentRepository documentRepository;

    public DocumentController(IngestionService ingestionService,
                              RateLimitService rateLimitService,
                              SandboxService sandboxService ,
                              SandboxResolver sandboxResolver,
                              DocumentRepository documentRepository) {
        this.rateLimitService = rateLimitService;
        this.ingestionService = ingestionService;
        this.sandboxService = sandboxService;
        this.sandboxResolver = sandboxResolver;
        this.documentRepository = documentRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) throws Exception {
        UUID tenantId = sandboxResolver.resolve(request);

        if (!sandboxService.isValid(tenantId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Sandbox expired."
                    ));
        }
        String ip = extractClientIp(request);

        if (!rateLimitService.tryConsume(ip, RateLimitType.UPLOAD)) {
            ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Upload rate limit exceeded."
                    ));
        }

        byte[] header = file.getBytes();

        if (header.length < 4 ||
                header[0] != '%' ||
                header[1] != 'P' ||
                header[2] != 'D' ||
                header[3] != 'F') {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                    "error", "Invalid PDF file."
            ));
        }


        byte[] fileBytes = file.getBytes();
        String hash = HashUtils.sha256(fileBytes);

        if (documentRepository.existsByTenantIdAndFileHash(tenantId, hash)) {
            return ResponseEntity
                    .badRequest().body(Map.of(
                            "error","This document has already been uploaded."
                    ));
        }

        UUID documentId =
                ingestionService.createDocument(file, tenantId,hash);

        ingestionService.ingestPdfAsync(
                header, //filebytes
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

    private String extractClientIp(HttpServletRequest request) {

        String header = request.getHeader("X-Forwarded-For");

        if (header != null && !header.isEmpty()) {
            return header.split(",")[0];
        }

        return request.getRemoteAddr();
    }
}