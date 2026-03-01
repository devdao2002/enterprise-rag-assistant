package com.ducdo.ai_assistant.security.exception;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiErrorResponse {

    private String status;
    private String message;
    private int code;
}