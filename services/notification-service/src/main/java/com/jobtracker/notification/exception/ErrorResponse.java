package com.jobtracker.notification.exception;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String timestamp,
        String path
) {}
