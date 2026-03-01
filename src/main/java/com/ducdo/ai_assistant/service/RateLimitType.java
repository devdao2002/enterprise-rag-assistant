package com.ducdo.ai_assistant.service;

public enum RateLimitType {

    ASK("Query rate limit exceeded."),
    UPLOAD("Upload rate limit exceeded.");

    private final String message;

    RateLimitType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}