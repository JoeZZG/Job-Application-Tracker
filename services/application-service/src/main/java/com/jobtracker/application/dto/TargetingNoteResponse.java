package com.jobtracker.application.dto;

import java.time.LocalDateTime;

public record TargetingNoteResponse(
        Long id,
        Long applicationId,
        String mustHaveKeywords,
        String niceToHaveKeywords,
        String customBulletIdeas,
        String jobDescriptionExcerpt,
        String matchNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
