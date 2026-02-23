package com.jobtracker.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_targeting_notes")
public class ResumeTargetingNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "must_have_keywords", columnDefinition = "TEXT")
    private String mustHaveKeywords;

    @Column(name = "nice_to_have_keywords", columnDefinition = "TEXT")
    private String niceToHaveKeywords;

    @Column(name = "custom_bullet_ideas", columnDefinition = "TEXT")
    private String customBulletIdeas;

    @Column(name = "job_description_excerpt", columnDefinition = "TEXT")
    private String jobDescriptionExcerpt;

    @Column(name = "match_notes", columnDefinition = "TEXT")
    private String matchNotes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters and setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getMustHaveKeywords() {
        return mustHaveKeywords;
    }

    public void setMustHaveKeywords(String mustHaveKeywords) {
        this.mustHaveKeywords = mustHaveKeywords;
    }

    public String getNiceToHaveKeywords() {
        return niceToHaveKeywords;
    }

    public void setNiceToHaveKeywords(String niceToHaveKeywords) {
        this.niceToHaveKeywords = niceToHaveKeywords;
    }

    public String getCustomBulletIdeas() {
        return customBulletIdeas;
    }

    public void setCustomBulletIdeas(String customBulletIdeas) {
        this.customBulletIdeas = customBulletIdeas;
    }

    public String getJobDescriptionExcerpt() {
        return jobDescriptionExcerpt;
    }

    public void setJobDescriptionExcerpt(String jobDescriptionExcerpt) {
        this.jobDescriptionExcerpt = jobDescriptionExcerpt;
    }

    public String getMatchNotes() {
        return matchNotes;
    }

    public void setMatchNotes(String matchNotes) {
        this.matchNotes = matchNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
