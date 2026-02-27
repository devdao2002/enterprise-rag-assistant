package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.service.IngestionService;
import com.ducdo.ai_assistant.service.RateLimitService;
import com.ducdo.ai_assistant.service.RateLimitType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final IngestionService ingestionService;
    private final RateLimitService rateLimitService;

    public DocumentController(IngestionService ingestionService, RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
                         @RequestParam UUID tenantId,
                         HttpServletRequest request) throws Exception {
        String ip = extractClientIp(request);

        if (!rateLimitService.tryConsume(ip, RateLimitType.UPLOAD)) {
            return ResponseEntity
                    .status(429)
                    .body("Upload rate limit exceeded.");
        }

        byte[] header = file.getBytes();
        if (header.length < 4 ||
                header[0] != '%' ||
                header[1] != 'P' ||
                header[2] != 'D' ||
                header[3] != 'F') {

            return ResponseEntity
                    .badRequest()
                    .body("Invalid PDF file.");
        }

        ingestionService.ingestPdf(file, tenantId);

        return ResponseEntity.ok("Document processed successfully.");
    }

    private String extractClientIp(HttpServletRequest request) {

        String header = request.getHeader("X-Forwarded-For");

        if (header != null && !header.isEmpty()) {
            return header.split(",")[0];
        }

        return request.getRemoteAddr();
    }
}