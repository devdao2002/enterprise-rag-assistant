package com.ducdo.ai_assistant.service;

import com.ducdo.ai_assistant.config.RateLimitProperties;
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

    private final RateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitProperties properties) {
        this.properties = properties;
    }

    private Bucket createBucket(RateLimitType type) {

        int limit;

        switch (type) {
            case ASK -> limit = properties.getAskPer10Minutes();
            case UPLOAD -> limit = properties.getUploadPer10Minutes();
            default -> throw new IllegalArgumentException("Unknown type");
        }

        log.info("Creating bucket for type={} limit={}", type, limit);

        Bandwidth bandwidth = Bandwidth.classic(
                limit,
                Refill.intervally(limit, Duration.ofMinutes(10))
        );

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    public boolean tryConsume(String ip, RateLimitType type) {

        String key = ip + ":" + type.name();

        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(type));

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