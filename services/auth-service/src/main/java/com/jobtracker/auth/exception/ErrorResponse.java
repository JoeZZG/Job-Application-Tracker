package com.jobtracker.auth.exception;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String timestamp,
        String path
) {}
