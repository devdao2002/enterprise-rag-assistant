package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.security.exception.ErrorResponseWriter;
import com.ducdo.ai_assistant.security.filter.RateLimitFilter;
import com.ducdo.ai_assistant.security.filter.SandboxFilter;
import com.ducdo.ai_assistant.service.RagService;
import com.ducdo.ai_assistant.service.RateLimitService;
import com.ducdo.ai_assistant.service.SandboxService;
import com.ducdo.ai_assistant.security.resolver.SandboxResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AskController.class)
@AutoConfigureMockMvc(addFilters = false)
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

    // Required for filter bean dependency resolution even with addFilters=false
    @MockitoBean
    private SandboxFilter sandboxFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private ErrorResponseWriter errorResponseWriter;

    @Test
    void ask_validRequest_shouldReturnAnswer() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(sandboxResolver.resolve(any())).thenReturn(tenantId);
        when(sandboxService.isValid(tenantId)).thenReturn(true);

        SseEmitter emitter = new SseEmitter();
        when(ragService.askStream("What is Spring?", tenantId)).thenReturn(emitter);

        mockMvc.perform(get("/api/ask/stream").param("question", "What is Spring?"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void ask_sandboxExpired_shouldSendExpiredMessageViaSse() throws Exception {
        UUID tenantId = UUID.randomUUID();
        when(sandboxResolver.resolve(any())).thenReturn(tenantId);
        when(sandboxService.isValid(tenantId)).thenReturn(false);

        // When sandbox is expired, the controller creates an SseEmitter,
        // sends "Sandbox expired." and completes it synchronously.
        mockMvc.perform(get("/api/ask/stream").param("question", "Test?"))
                .andExpect(status().isOk());
    }
}
