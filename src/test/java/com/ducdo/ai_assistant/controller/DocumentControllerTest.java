package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.security.exception.ErrorResponseWriter;
import com.ducdo.ai_assistant.security.filter.RateLimitFilter;
import com.ducdo.ai_assistant.security.filter.SandboxFilter;
import com.ducdo.ai_assistant.service.IngestionService;
import com.ducdo.ai_assistant.security.resolver.SandboxResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private IngestionService ingestionService;

        @MockitoBean
        private com.ducdo.ai_assistant.repository.DocumentRepository documentRepository;

        @MockitoBean
        private SandboxResolver sandboxResolver;

        // Required for filter bean dependency resolution even with addFilters=false
        @MockitoBean
        private SandboxFilter sandboxFilter;

        @MockitoBean
        private RateLimitFilter rateLimitFilter;

        @MockitoBean
        private ErrorResponseWriter errorResponseWriter;

        @Test
        void upload_validPdf_shouldReturnOk() throws Exception {
                UUID tenantId = UUID.randomUUID();
                when(sandboxResolver.resolve(any())).thenReturn(tenantId);
                when(documentRepository.existsByTenantIdAndFileHash(eq(tenantId), anyString())).thenReturn(false);
                when(ingestionService.createDocument(any(), eq(tenantId), anyString())).thenReturn(UUID.randomUUID());

                byte[] pdfHeader = { 0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E }; // %PDF-1.
                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, pdfHeader);

                mockMvc.perform(multipart("/api/documents/upload").file(file))
                                .andExpect(status().isOk())
                                .andExpect(content().string(
                                                org.hamcrest.Matchers.containsString("\"status\":\"PROCESSING\"")))
                                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"documentId\"")));
        }

        @Test
        void upload_invalidPdfContent_shouldReturn429WithErrorMessage() throws Exception {
                UUID tenantId = UUID.randomUUID();
                when(sandboxResolver.resolve(any())).thenReturn(tenantId);

                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "not a pdf".getBytes());

                mockMvc.perform(multipart("/api/documents/upload").file(file))
                                .andExpect(status().is(429))
                                .andExpect(content().string(org.hamcrest.Matchers
                                                .containsString("\"message\":\"Invalid PDF file.\"")));
        }

        @Test
        void upload_duplicateDocument_shouldReturn429() throws Exception {
                UUID tenantId = UUID.randomUUID();
                when(sandboxResolver.resolve(any())).thenReturn(tenantId);
                when(documentRepository.existsByTenantIdAndFileHash(eq(tenantId), anyString())).thenReturn(true);

                byte[] pdfHeader = { 0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E }; // %PDF-1.
                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, pdfHeader);

                mockMvc.perform(multipart("/api/documents/upload").file(file))
                                .andExpect(status().is(429))
                                .andExpect(content().string(org.hamcrest.Matchers
                                                .containsString("\"message\":\"This document has already been uploaded.\"")));
        }
}
