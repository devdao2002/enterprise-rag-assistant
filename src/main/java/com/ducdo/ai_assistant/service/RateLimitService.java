package com.ducdo.ai_assistant.service;

import io.github.bucket4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(
                3,
                Refill.intervally(3, Duration.ofMinutes(10))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryConsume(String ip) {

        Bucket bucket = buckets.computeIfAbsent(ip, k -> {
            log.info("Creating new rate limit bucket for IP={}", ip);
            return createBucket();
        });

        boolean consumed = bucket.tryConsume(1);

        if (consumed) {
            long remaining = bucket.getAvailableTokens();
            log.info("IP={} consumed token. Remaining={}", ip, remaining);
        } else {
            log.warn("IP={} blocked by rate limit", ip);
        }

        return consumed;
    }
}
