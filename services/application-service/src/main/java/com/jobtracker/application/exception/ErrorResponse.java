package com.jobtracker.application.exception;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String timestamp,
        String path
) {}
