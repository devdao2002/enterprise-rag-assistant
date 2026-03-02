package com.ducdo.ai_assistant.dto.bootstrap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class VersionDto {

    private String version;
    private String commitFull;
    private String commitShort;
    private String commitUrl;
    private String buildTime;
}