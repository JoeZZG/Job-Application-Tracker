package com.jobtracker.application.dto;

import com.jobtracker.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateApplicationRequest(
        @NotBlank String company,
        @NotBlank String jobTitle,
        String location,
        String jobPostUrl,
        LocalDate deadline,
        LocalDate appliedDate,
        ApplicationStatus status,
        String notes
) {}
