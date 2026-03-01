package com.ducdo.ai_assistant.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    @Value("${app.version}")
    private String version;

    @Value("${app.git.commit:unknown}")
    private String gitCommit;

    @Value("${app.build.time:unknown}")
    private String buildTime;

    @GetMapping
    public Map<String, String> version() {

        String shortCommit =
                gitCommit.length() >= 7
                        ? gitCommit.substring(0, 7)
                        : gitCommit;

        return Map.of(
                "version", version,
                "commitFull", gitCommit,
                "commitShort", shortCommit,
                "commitUrl",
                "https://github.com/devdao2002/enterprise-rag-assistant/commit/" + gitCommit,
                "buildTime", buildTime
        );
    }
}
