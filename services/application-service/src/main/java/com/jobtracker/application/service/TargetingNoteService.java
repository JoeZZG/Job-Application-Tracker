package com.jobtracker.application.service;

import com.jobtracker.application.dto.TargetingNoteRequest;
import com.jobtracker.application.dto.TargetingNoteResponse;
import com.jobtracker.application.entity.JobApplication;
import com.jobtracker.application.entity.ResumeTargetingNote;
import com.jobtracker.application.exception.ForbiddenException;
import com.jobtracker.application.exception.ResourceNotFoundException;
import com.jobtracker.application.repository.JobApplicationRepository;
import com.jobtracker.application.repository.ResumeTargetingNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TargetingNoteService {

    private final ResumeTargetingNoteRepository noteRepository;
    private final JobApplicationRepository applicationRepository;

    public TargetingNoteService(
            ResumeTargetingNoteRepository noteRepository,
            JobApplicationRepository applicationRepository) {
        this.noteRepository = noteRepository;
        this.applicationRepository = applicationRepository;
    }

    public TargetingNoteResponse get(Long userId, Long applicationId) {
        verifyOwnership(userId, applicationId);

        ResumeTargetingNote note = noteRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Targeting note not found"));

        return toResponse(note);
    }

    @Transactional
    public TargetingNoteResponse upsert(Long userId, Long applicationId, TargetingNoteRequest req) {
        verifyOwnership(userId, applicationId);

        // Find existing note or create a new one bound to this application.
        ResumeTargetingNote note = noteRepository.findByApplicationId(applicationId)
                .orElseGet(() -> {
                    ResumeTargetingNote n = new ResumeTargetingNote();
                    n.setApplicationId(applicationId);
                    return n;
                });

        note.setMustHaveKeywords(req.mustHaveKeywords());
        note.setNiceToHaveKeywords(req.niceToHaveKeywords());
        note.setCustomBulletIdeas(req.customBulletIdeas());
        note.setJobDescriptionExcerpt(req.jobDescriptionExcerpt());
        note.setMatchNotes(req.matchNotes());

        ResumeTargetingNote saved = noteRepository.save(note);
        return toResponse(saved);
    }

    // --- Private helpers ---

    private void verifyOwnership(Long userId, Long applicationId) {
        JobApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!app.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own this application");
        }
    }

    private TargetingNoteResponse toResponse(ResumeTargetingNote note) {
        return new TargetingNoteResponse(
                note.getId(),
                note.getApplicationId(),
                note.getMustHaveKeywords(),
                note.getNiceToHaveKeywords(),
                note.getCustomBulletIdeas(),
                note.getJobDescriptionExcerpt(),
                note.getMatchNotes(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
