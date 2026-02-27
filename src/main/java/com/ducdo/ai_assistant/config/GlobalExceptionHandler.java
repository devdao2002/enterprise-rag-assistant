package com.ducdo.ai_assistant.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(
            MaxUploadSizeExceededException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("File too large. Maximum allowed size is 5MB.");
    }
}