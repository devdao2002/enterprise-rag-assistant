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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
        when(ragService.ask("What is Spring?", tenantId)).thenReturn("Spring is a Java framework.");

        mockMvc.perform(get("/api/ask").param("question", "What is Spring?"))
                .andExpect(status().isOk())
                .andExpect(content().string("Spring is a Java framework."));
    }

    @Test
    void ask_sandboxExpired_shouldReturnBadRequest() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(sandboxResolver.resolve(any())).thenReturn(tenantId);
        when(sandboxService.isValid(tenantId)).thenReturn(false);

        mockMvc.perform(get("/api/ask").param("question", "Test?"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Sandbox expired."));
    }

    @Test
    void ask_rateLimitExceeded_shouldReturn429() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(sandboxResolver.resolve(any())).thenReturn(tenantId);
        when(sandboxService.isValid(tenantId)).thenReturn(true);
        when(rateLimitService.tryConsume(anyString(), eq(RateLimitType.ASK))).thenReturn(false);

        mockMvc.perform(get("/api/ask").param("question", "Test?"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("Too many requests. Please try again later."));
    }
}
