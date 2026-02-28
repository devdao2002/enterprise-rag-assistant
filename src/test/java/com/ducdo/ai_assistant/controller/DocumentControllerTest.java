package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.service.IngestionService;
import com.ducdo.ai_assistant.service.RateLimitService;
import com.ducdo.ai_assistant.service.RateLimitType;
import com.ducdo.ai_assistant.service.SandboxService;
import com.ducdo.ai_assistant.util.SandboxResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private IngestionService ingestionService;

        @MockitoBean
        private RateLimitService rateLimitService;

        @MockitoBean
        private SandboxService sandboxService;

        @MockitoBean
        private SandboxResolver sandboxResolver;

        @Test
        void upload_validPdf_shouldReturnOk() throws Exception {
                UUID tenantId = UUID.randomUUID();
                when(sandboxResolver.resolve(any())).thenReturn(tenantId);
                when(sandboxService.isValid(tenantId)).thenReturn(true);
                when(rateLimitService.tryConsume(anyString(), eq(RateLimitType.UPLOAD))).thenReturn(true);
                doNothing().when(ingestionService).ingestPdf(any(), eq(tenantId));

                byte[] pdfHeader = { 0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E }; // %PDF-1.
                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, pdfHeader);

                mockMvc.perform(multipart("/api/documents/upload").file(file))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Document processed successfully."));
        }

        @Test
        void upload_invalidPdfContent_shouldReturnBadRequest() throws Exception {
                UUID tenantId = UUID.randomUUID();
                when(sandboxResolver.resolve(any())).thenReturn(tenantId);
                when(sandboxService.isValid(tenantId)).thenReturn(true);
                when(rateLimitService.tryConsume(anyString(), eq(RateLimitType.UPLOAD))).thenReturn(true);

                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "not a pdf".getBytes());

                mockMvc.perform(multipart("/api/documents/upload").file(file))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Invalid PDF file."));
        }
}
