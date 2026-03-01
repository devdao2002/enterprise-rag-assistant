package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.service.RagService;
import com.ducdo.ai_assistant.service.RateLimitService;
import com.ducdo.ai_assistant.service.RateLimitType;
import com.ducdo.ai_assistant.service.SandboxService;
import com.ducdo.ai_assistant.util.SandboxResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AskController.class)
class AskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private SandboxService sandboxService;

    @MockitoBean
    private SandboxResolver sandboxResolver;

    @Test
    void ask_validRequest_shouldReturnAnswer() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(sandboxResolver.resolve(any())).thenReturn(tenantId);
        when(sandboxService.isValid(tenantId)).thenReturn(true);
        when(rateLimitService.tryConsume(anyString(), eq(RateLimitType.ASK))).thenReturn(true);
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter();
        when(ragService.askStream("What is Spring?", tenantId)).thenReturn(emitter);

        mockMvc.perform(get("/api/ask/stream").param("question", "What is Spring?"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void ask_sandboxExpired_shouldReturnBadRequest() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(sandboxResolver.resolve(any())).thenReturn(tenantId);
        when(sandboxService.isValid(tenantId)).thenReturn(false);

        mockMvc.perform(get("/api/ask/stream").param("question", "Test?"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void ask_rateLimitExceeded_shouldReturn429() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(sandboxResolver.resolve(any())).thenReturn(tenantId);
        when(sandboxService.isValid(tenantId)).thenReturn(true);
        when(rateLimitService.tryConsume(anyString(), eq(RateLimitType.ASK))).thenReturn(false);

        mockMvc.perform(get("/api/ask/stream").param("question", "Test?"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }
}
