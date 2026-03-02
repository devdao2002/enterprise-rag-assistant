package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.dto.bootstrap.VersionDto;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class VersionServiceTest {

    @Test
    void getVersionInfo_shouldReturnCorrectDto() throws Exception {
        VersionService service = new VersionService();

        // Set @Value fields via reflection (unit test, no Spring context)
        setField(service, "version", "2.0.0");
        setField(service, "gitCommit", "abcdef1234567890");
        setField(service, "buildTime", "2026-03-01T10:00:00Z");

        VersionDto dto = service.getVersionInfo();

        assertThat(dto.getVersion()).isEqualTo("2.0.0");
        assertThat(dto.getCommitFull()).isEqualTo("abcdef1234567890");
        assertThat(dto.getCommitShort()).isEqualTo("abcdef1");
        assertThat(dto.getCommitUrl()).isEqualTo(
                "https://github.com/devdao2002/enterprise-rag-assistant/commit/abcdef1234567890");
        assertThat(dto.getBuildTime()).isEqualTo("2026-03-01T10:00:00Z");
    }

    @Test
    void getVersionInfo_shortCommit_shouldHandleShortHash() throws Exception {
        VersionService service = new VersionService();

        setField(service, "version", "1.0.0");
        setField(service, "gitCommit", "abc");
        setField(service, "buildTime", "unknown");

        VersionDto dto = service.getVersionInfo();

        // If commit is shorter than 7 chars, use as-is
        assertThat(dto.getCommitShort()).isEqualTo("abc");
        assertThat(dto.getCommitFull()).isEqualTo("abc");
    }

    @Test
    void getVersionInfo_unknownDefaults_shouldReturnUnknown() throws Exception {
        VersionService service = new VersionService();

        setField(service, "version", "1.0.0");
        setField(service, "gitCommit", "unknown");
        setField(service, "buildTime", "unknown");

        VersionDto dto = service.getVersionInfo();

        assertThat(dto.getCommitFull()).isEqualTo("unknown");
        assertThat(dto.getCommitShort()).isEqualTo("unknown");
        assertThat(dto.getBuildTime()).isEqualTo("unknown");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
