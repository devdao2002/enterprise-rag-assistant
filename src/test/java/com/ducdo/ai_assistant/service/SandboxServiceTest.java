package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.model.SandboxSession;
import com.ducdo.ai_assistant.repository.SandboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SandboxServiceTest {

    private SandboxRepository repository;
    private SandboxService service;

    @BeforeEach
    void setUp() {
        repository = mock(SandboxRepository.class);
        service = new SandboxService(repository);
    }

    @Test
    void isValid_sandboxExistsAndNotExpired_returnsTrue() {
        UUID tenantId = UUID.randomUUID();
        SandboxSession session = SandboxSession.builder()
                .tenantId(tenantId)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(repository.findById(tenantId)).thenReturn(Optional.of(session));

        assertThat(service.isValid(tenantId)).isTrue();
    }

    @Test
    void isValid_sandboxExistsAndExpired_returnsFalse() {
        UUID tenantId = UUID.randomUUID();
        SandboxSession session = SandboxSession.builder()
                .tenantId(tenantId)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(repository.findById(tenantId)).thenReturn(Optional.of(session));

        assertThat(service.isValid(tenantId)).isFalse();
    }

    @Test
    void isValid_sandboxDoesNotExist_returnsFalse() {
        UUID tenantId = UUID.randomUUID();
        when(repository.findById(tenantId)).thenReturn(Optional.empty());

        assertThat(service.isValid(tenantId)).isFalse();
    }
}
