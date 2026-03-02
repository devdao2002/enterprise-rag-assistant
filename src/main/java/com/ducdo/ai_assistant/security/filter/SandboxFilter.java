package com.ducdo.ai_assistant.security.filter;

import com.ducdo.ai_assistant.security.context.TenantContext;
import com.ducdo.ai_assistant.security.exception.ErrorResponseWriter;
import com.ducdo.ai_assistant.security.resolver.SandboxResolver;
import com.ducdo.ai_assistant.service.SandboxService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SandboxFilter extends OncePerRequestFilter {

    private final SandboxResolver sandboxResolver;
    private final SandboxService sandboxService;
    private final ErrorResponseWriter errorWriter;
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/version",
            "/api/sandbox",
            "/api/sandbox/validate"
    );
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!path.startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            UUID tenantId = sandboxResolver.resolve(request);

            if (!sandboxService.isValid(tenantId)) {
                errorWriter.write(
                        response,
                        400,
                        "Sandbox expired.",
                        request.getRequestURI()
                );
                return;
            }

            TenantContext.set(tenantId);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            errorWriter.write(
                    response,
                    400,
                    "Invalid sandbox token.",
                    request.getRequestURI()
            );
        } finally {
            TenantContext.clear();
        }
    }

    private void sendError(HttpServletResponse response,
                           int status,
                           String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"" + message + "\"}"
        );
    }
}