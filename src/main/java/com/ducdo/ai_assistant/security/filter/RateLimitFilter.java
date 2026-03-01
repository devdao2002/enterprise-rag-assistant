package com.ducdo.ai_assistant.security.filter;

import com.ducdo.ai_assistant.security.exception.ErrorResponseWriter;
import com.ducdo.ai_assistant.service.RateLimitService;
import com.ducdo.ai_assistant.service.RateLimitType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ErrorResponseWriter errorWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String ip = extractIp(request);

        RateLimitType type = resolveType(request.getRequestURI());

        if (type != null &&
                !rateLimitService.tryConsume(ip, type)) {

            errorWriter.write(
                    response,
                    429,
                    type.getMessage(),
                    request.getRequestURI()
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private RateLimitType resolveType(String uri) {
        if (uri.contains("/ask")) return RateLimitType.ASK;
        if (uri.contains("/upload")) return RateLimitType.UPLOAD;
        return null;
    }

    private String extractIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        return (header != null) ? header.split(",")[0]
                : request.getRemoteAddr();
    }
}