package com.ducdo.ai_assistant.dto.bootstrap;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BootstrapResponse {

    private VersionDto version;
    private SandboxDto sandbox;
    private boolean documentReady;
}
