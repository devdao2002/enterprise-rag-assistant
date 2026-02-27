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

    private static final Logger log =
            LoggerFactory.getLogger(RateLimitService.class);

    // Key format: ip + type
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket(RateLimitType type) {

        Bandwidth limit;

        switch (type) {

            case ASK -> limit = Bandwidth.classic(
                    20,
                    Refill.intervally(20, Duration.ofMinutes(10))
            );

            case UPLOAD -> limit = Bandwidth.classic(
                    2,
                    Refill.intervally(2, Duration.ofMinutes(10))
            );

            default -> throw new IllegalArgumentException("Unknown type");
        }

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryConsume(String ip, RateLimitType type) {

        String key = ip + ":" + type.name();

        Bucket bucket = buckets.computeIfAbsent(key, k -> {
            log.info("Creating bucket for key={}", key);
            return createBucket(type);
        });

        boolean consumed = bucket.tryConsume(1);

        if (consumed) {
            log.info("Allowed key={} remaining={}",
                    key,
                    bucket.getAvailableTokens());
        } else {
            log.warn("Blocked key={}", key);
        }

        return consumed;
    }
}