package com.jobtracker.application.service;

import com.jobtracker.application.config.CacheNames;
import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.application.entity.ApplicationStatus;
import com.jobtracker.application.entity.JobApplication;
import com.jobtracker.application.exception.ForbiddenException;
import com.jobtracker.application.exception.ResourceNotFoundException;
import com.jobtracker.application.messaging.DeadlineEventPublisher;
import com.jobtracker.application.repository.JobApplicationRepository;
import com.jobtracker.application.repository.ResumeTargetingNoteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final ResumeTargetingNoteRepository targetingNoteRepository;
    private final DeadlineEventPublisher deadlineEventPublisher;

    public ApplicationService(
            JobApplicationRepository applicationRepository,
            ResumeTargetingNoteRepository targetingNoteRepository,
            DeadlineEventPublisher deadlineEventPublisher) {
        this.applicationRepository = applicationRepository;
        this.targetingNoteRepository = targetingNoteRepository;
        this.deadlineEventPublisher = deadlineEventPublisher;
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.DASHBOARD_SUMMARY, key = "#userId")
    public ApplicationResponse create(Long userId, CreateApplicationRequest req) {
        JobApplication app = new JobApplication();
        app.setUserId(userId);
        app.setCompany(req.company());
        app.setJobTitle(req.jobTitle());
        app.setLocation(req.location());
        app.setJobPostUrl(req.jobPostUrl());
        app.setDeadline(req.deadline());
        app.setAppliedDate(req.appliedDate());
        // Default to SAVED if caller did not supply a status.
        app.setStatus(req.status() != null ? req.status() : ApplicationStatus.SAVED);
        app.setNotes(req.notes());

        JobApplication saved = applicationRepository.save(app);

        if (saved.getDeadline() != null) {
            deadlineEventPublisher.publishDeadlineEvent(saved);
        }

        return toResponse(saved);
    }

    public List<ApplicationResponse> list(Long userId, ApplicationStatus status) {
        List<JobApplication> applications = (status == null)
                ? applicationRepository.findByUserId(userId)
                : applicationRepository.findByUserIdAndStatus(userId, status);

        return applications.stream()
                .map(this::toResponse)
                .toList();
    }

    public ApplicationResponse getById(Long userId, Long id) {
        JobApplication app = loadAndVerifyOwnership(userId, id);
        return toResponse(app);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.DASHBOARD_SUMMARY, key = "#userId")
    public ApplicationResponse update(Long userId, Long id, UpdateApplicationRequest req) {
        JobApplication app = loadAndVerifyOwnership(userId, id);

        // Capture the deadline before applying changes to detect if it changed.
        var previousDeadline = app.getDeadline();

        // Apply only non-null fields — partial update semantics.
        if (req.company() != null) {
            app.setCompany(req.company());
        }
        if (req.jobTitle() != null) {
            app.setJobTitle(req.jobTitle());
        }
        if (req.location() != null) {
            app.setLocation(req.location());
        }
        if (req.jobPostUrl() != null) {
            app.setJobPostUrl(req.jobPostUrl());
        }
        if (req.deadline() != null) {
            app.setDeadline(req.deadline());
        }
        if (req.appliedDate() != null) {
            app.setAppliedDate(req.appliedDate());
        }
        if (req.status() != null) {
            app.setStatus(req.status());
        }
        if (req.notes() != null) {
            app.setNotes(req.notes());
        }

        JobApplication saved = applicationRepository.save(app);

        // Publish a deadline event only when the deadline field was actually changed
        // and the new value is non-null.
        boolean deadlineChanged = req.deadline() != null
                && !req.deadline().equals(previousDeadline);
        if (deadlineChanged) {
            deadlineEventPublisher.publishDeadlineEvent(saved);
        }

        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.DASHBOARD_SUMMARY, key = "#userId")
    public void delete(Long userId, Long id) {
        JobApplication app = loadAndVerifyOwnership(userId, id);
        // The cascade delete on resume_targeting_notes is handled at the DB level (FK + ON DELETE CASCADE).
        applicationRepository.delete(app);
    }

    // --- Private helpers ---

    private JobApplication loadAndVerifyOwnership(Long userId, Long id) {
        JobApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!app.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own this application");
        }

        return app;
    }

    private ApplicationResponse toResponse(JobApplication app) {
        return new ApplicationResponse(
                app.getId(),
                app.getUserId(),
                app.getCompany(),
                app.getJobTitle(),
                app.getLocation(),
                app.getJobPostUrl(),
                app.getDeadline(),
                app.getAppliedDate(),
                app.getStatus(),
                app.getNotes(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
