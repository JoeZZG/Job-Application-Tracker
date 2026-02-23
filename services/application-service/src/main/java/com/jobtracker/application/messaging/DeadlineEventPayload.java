package com.jobtracker.application.messaging;

import java.time.LocalDate;

public record DeadlineEventPayload(
        Long applicationId,
        Long userId,
        String company,
        String jobTitle,
        LocalDate deadline,
        String eventType,
        String timestamp
) {}
