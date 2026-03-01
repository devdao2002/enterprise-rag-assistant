package com.ducdo.ai_assistant.security.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SandboxResolver {

    public UUID resolve(HttpServletRequest request) {

        String token = request.getHeader("X-Sandbox-Token");

        if (token == null || token.isBlank()) {
            throw new RuntimeException("Missing sandbox token");
        }

        token = token.replace("\"", "").trim();

        try {
            return UUID.fromString(token);
        } catch (Exception e) {
            throw new RuntimeException("Invalid sandbox token: " + token);
        }
    }
}