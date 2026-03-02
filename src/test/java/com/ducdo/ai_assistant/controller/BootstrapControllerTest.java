package com.ducdo.ai_assistant.controller;

import com.ducdo.ai_assistant.dto.bootstrap.SandboxDto;
import com.ducdo.ai_assistant.dto.bootstrap.VersionDto;
import com.ducdo.ai_assistant.model.SandboxSession;
import com.ducdo.ai_assistant.security.exception.ErrorResponseWriter;
import com.ducdo.ai_assistant.security.filter.RateLimitFilter;
import com.ducdo.ai_assistant.security.filter.SandboxFilter;
import com.ducdo.ai_assistant.security.resolver.SandboxResolver;
import com.ducdo.ai_assistant.service.IngestionService;
import com.ducdo.ai_assistant.service.SandboxService;
import com.ducdo.ai_assistant.service.VersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BootstrapController.class)
@AutoConfigureMockMvc(addFilters = false)
class BootstrapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SandboxService sandboxService;

    @MockitoBean
    private SandboxResolver sandboxResolver;

    @MockitoBean
    private IngestionService ingestionService;

    @MockitoBean
    private VersionService versionService;

    // Required for filter bean dependency resolution
    @MockitoBean
    private SandboxFilter sandboxFilter;

    @MockitoBean
    private RateLimitFilter rateLimitFilter;

    @MockitoBean
    private ErrorResponseWriter errorResponseWriter;

    @Test
    void bootstrap_existingValidSandbox_shouldReturnFullPayload() throws Exception {
        UUID tenantId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(3);

        when(sandboxResolver.resolve(any())).thenReturn(tenantId);
        when(sandboxService.isValid(tenantId)).thenReturn(true);

        SandboxSession session = SandboxSession.builder()
                .tenantId(tenantId)
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .ipAddress("127.0.0.1")
                .build();
        when(sandboxService.getSandbox(tenantId)).thenReturn(session);

        when(ingestionService.hasReadyDocument(tenantId)).thenReturn(true);

        VersionDto versionDto = VersionDto.builder()
                .version("1.0.0")
                .commitFull("abc1234567890")
                .commitShort("abc1234")
                .commitUrl("https://github.com/devdao2002/enterprise-rag-assistant/commit/abc1234567890")
                .buildTime("2026-03-01")
                .build();
        when(versionService.getVersionInfo()).thenReturn(versionDto);

        mockMvc.perform(get("/api/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version.version", is("1.0.0")))
                .andExpect(jsonPath("$.version.commitShort", is("abc1234")))
                .andExpect(jsonPath("$.version.buildTime", is("2026-03-01")))
                .andExpect(jsonPath("$.sandbox.token", is(tenantId.toString())))
                .andExpect(jsonPath("$.sandbox.active", is(true)))
                .andExpect(jsonPath("$.sandbox.remainingSeconds", greaterThan(0)))
                .andExpect(jsonPath("$.documentReady", is(true)));

        // Should reuse existing sandbox, not create a new one
        verify(sandboxService, never()).createSandbox(any(), anyString());
    }

    @Test
    void bootstrap_noSandboxToken_shouldCreateNewSandbox() throws Exception {
        UUID newTenantId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(4);

        // Resolver throws exception (no token in header)
        when(sandboxResolver.resolve(any())).thenThrow(new RuntimeException("Missing sandbox token"));

        // After creation, getSandbox returns the new session
        SandboxSession session = SandboxSession.builder()
                .tenantId(newTenantId)
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .ipAddress("127.0.0.1")
                .build();
        when(sandboxService.getSandbox(any())).thenReturn(session);

        when(ingestionService.hasReadyDocument(any())).thenReturn(false);

        VersionDto versionDto = VersionDto.builder()
                .version("1.0.0")
                .commitFull("abc1234567890")
                .commitShort("abc1234")
                .commitUrl("https://github.com/devdao2002/enterprise-rag-assistant/commit/abc1234567890")
                .buildTime("2026-03-01")
                .build();
        when(versionService.getVersionInfo()).thenReturn(versionDto);

        mockMvc.perform(get("/api/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sandbox.active", is(true)))
                .andExpect(jsonPath("$.sandbox.remainingSeconds", greaterThan(0)))
                .andExpect(jsonPath("$.documentReady", is(false)));

        // Should create a new sandbox
        verify(sandboxService).createSandbox(any(), anyString());
    }

    @Test
    void bootstrap_expiredSandbox_shouldCreateNewSandbox() throws Exception {
        UUID expiredTenantId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(4);

        when(sandboxResolver.resolve(any())).thenReturn(expiredTenantId);
        when(sandboxService.isValid(expiredTenantId)).thenReturn(false);

        SandboxSession newSession = SandboxSession.builder()
                .tenantId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .ipAddress("127.0.0.1")
                .build();
        when(sandboxService.getSandbox(any())).thenReturn(newSession);

        when(ingestionService.hasReadyDocument(any())).thenReturn(false);

        VersionDto versionDto = VersionDto.builder()
                .version("1.0.0")
                .commitFull("abc1234567890")
                .commitShort("abc1234")
                .commitUrl("https://github.com/devdao2002/enterprise-rag-assistant/commit/abc1234567890")
                .buildTime("2026-03-01")
                .build();
        when(versionService.getVersionInfo()).thenReturn(versionDto);

        mockMvc.perform(get("/api/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sandbox.active", is(true)))
                .andExpect(jsonPath("$.documentReady", is(false)));

        // Should create a new sandbox since the existing one is expired
        verify(sandboxService).createSandbox(any(), anyString());
    }

    @Test
    void bootstrap_pastExpiry_shouldReturnZeroRemainingSeconds() throws Exception {
        UUID tenantId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().minusMinutes(1);

        when(sandboxResolver.resolve(any())).thenReturn(tenantId);
        when(sandboxService.isValid(tenantId)).thenReturn(true);

        SandboxSession session = SandboxSession.builder()
                .tenantId(tenantId)
                .createdAt(LocalDateTime.now().minusHours(1))
                .expiresAt(expiresAt)
                .ipAddress("127.0.0.1")
                .build();
        when(sandboxService.getSandbox(tenantId)).thenReturn(session);

        when(ingestionService.hasReadyDocument(tenantId)).thenReturn(false);

        VersionDto versionDto = VersionDto.builder()
                .version("1.0.0")
                .commitFull("abc1234567890")
                .commitShort("abc1234")
                .commitUrl("https://github.com/devdao2002/enterprise-rag-assistant/commit/abc1234567890")
                .buildTime("2026-03-01")
                .build();
        when(versionService.getVersionInfo()).thenReturn(versionDto);

        mockMvc.perform(get("/api/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sandbox.remainingSeconds", is(0)));
    }
}
