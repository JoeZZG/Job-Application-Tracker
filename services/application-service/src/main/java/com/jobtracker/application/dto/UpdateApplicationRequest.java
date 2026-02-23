package com.jobtracker.application.dto;

import com.jobtracker.application.entity.ApplicationStatus;

import java.time.LocalDate;

// All fields are nullable to support partial updates.
// Only non-null fields supplied by the caller will be applied to the entity.
public record UpdateApplicationRequest(
        String company,
        String jobTitle,
        String location,
        String jobPostUrl,
        LocalDate deadline,
        LocalDate appliedDate,
        ApplicationStatus status,
        String notes
) {}
