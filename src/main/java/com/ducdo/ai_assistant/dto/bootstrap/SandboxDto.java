package com.ducdo.ai_assistant.dto.bootstrap;

import java.util.UUID;

public record SandboxDto(
        UUID token,
        boolean active,
        long remainingSeconds
) {}
