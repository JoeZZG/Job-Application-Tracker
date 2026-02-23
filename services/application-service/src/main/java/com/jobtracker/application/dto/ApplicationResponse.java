package com.jobtracker.application.dto;

import com.jobtracker.application.entity.ApplicationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ApplicationResponse(
        Long id,
        Long userId,
        String company,
        String jobTitle,
        String location,
        String jobPostUrl,
        LocalDate deadline,
        LocalDate appliedDate,
        ApplicationStatus status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
