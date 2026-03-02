package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.dto.bootstrap.VersionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VersionService {

    @Value("${app.version}")
    private String version;

    @Value("${app.git.commit:unknown}")
    private String gitCommit;

    @Value("${app.build.time:unknown}")
    private String buildTime;

    private static final String GITHUB_BASE =
            "https://github.com/devdao2002/enterprise-rag-assistant/commit/";

    public VersionDto getVersionInfo() {

        String shortCommit =
                gitCommit.length() >= 7
                        ? gitCommit.substring(0, 7)
                        : gitCommit;

        return VersionDto.builder()
                .version(version)
                .commitFull(gitCommit)
                .commitShort(shortCommit)
                .commitUrl(
                        "https://github.com/devdao2002/enterprise-rag-assistant/commit/" + gitCommit
                )
                .buildTime(buildTime)
                .build();
    }

    private String safe(String value) {
        return value == null ? "unknown" : value;
    }
}