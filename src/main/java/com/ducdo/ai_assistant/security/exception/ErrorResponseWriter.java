package com.ducdo.ai_assistant.security.exception;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response,
                      int status,
                      String message,
                      String path) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "status", "ERROR",
                "message", message,
                "code", status,
                "timestamp", LocalDateTime.now(),
                "path", path
        );

        response.getWriter().write(
                objectMapper.writeValueAsString(body)
        );
    }
}