package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.service.IngestionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final IngestionService ingestionService;

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam UUID tenantId) throws Exception {

        ingestionService.ingestPdf(file, tenantId);

        return "Document processed successfully.";
    }
}