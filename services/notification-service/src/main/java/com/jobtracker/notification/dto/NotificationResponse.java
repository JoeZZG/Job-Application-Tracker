package com.jobtracker.notification.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long userId,
        String type,
        String title,
        String message,
        boolean isRead,
        LocalDateTime createdAt
) {}
