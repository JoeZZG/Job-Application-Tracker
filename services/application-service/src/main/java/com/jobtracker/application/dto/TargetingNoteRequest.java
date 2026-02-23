package com.jobtracker.application.dto;

public record TargetingNoteRequest(
        String mustHaveKeywords,
        String niceToHaveKeywords,
        String customBulletIdeas,
        String jobDescriptionExcerpt,
        String matchNotes
) {}
