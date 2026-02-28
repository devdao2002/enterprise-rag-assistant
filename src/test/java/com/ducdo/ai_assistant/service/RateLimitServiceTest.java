package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitServiceTest {

    private RateLimitProperties properties;
    private RateLimitService service;

    @BeforeEach
    void setUp() {
        properties = mock(RateLimitProperties.class);
        when(properties.getAskPer10Minutes()).thenReturn(2);
        when(properties.getUploadPer10Minutes()).thenReturn(1);
        service = new RateLimitService(properties);
    }

    @Test
    void tryConsume_ask_shouldAllowUpToLimit() {
        String ip = "192.168.1.1";

        boolean allowed1 = service.tryConsume(ip, RateLimitType.ASK);
        assertThat(allowed1).isTrue();

        boolean allowed2 = service.tryConsume(ip, RateLimitType.ASK);
        assertThat(allowed2).isTrue();

        boolean allowed3 = service.tryConsume(ip, RateLimitType.ASK);
        assertThat(allowed3).isFalse();
    }

    @Test
    void tryConsume_upload_shouldAllowUpToLimit() {
        String ip = "192.168.1.1";

        boolean allowed1 = service.tryConsume(ip, RateLimitType.UPLOAD);
        assertThat(allowed1).isTrue();

        boolean allowed2 = service.tryConsume(ip, RateLimitType.UPLOAD);
        assertThat(allowed2).isFalse();
    }
}
