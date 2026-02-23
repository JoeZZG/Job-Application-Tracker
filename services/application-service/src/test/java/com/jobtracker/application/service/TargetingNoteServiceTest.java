package com.jobtracker.application.service;

import com.jobtracker.application.dto.TargetingNoteRequest;
import com.jobtracker.application.dto.TargetingNoteResponse;
import com.jobtracker.application.entity.JobApplication;
import com.jobtracker.application.entity.ResumeTargetingNote;
import com.jobtracker.application.exception.ResourceNotFoundException;
import com.jobtracker.application.repository.JobApplicationRepository;
import com.jobtracker.application.repository.ResumeTargetingNoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TargetingNoteServiceTest {

    @Mock
    private ResumeTargetingNoteRepository noteRepository;

    @Mock
    private JobApplicationRepository applicationRepository;

    @InjectMocks
    private TargetingNoteService targetingNoteService;

    private static final Long USER_ID = 1L;
    private static final Long APPLICATION_ID = 10L;
    private static final Long NOTE_ID = 20L;

    // Creates a JobApplication owned by USER_ID — simulates the ownership check passing.
    private JobApplication ownedApplication() {
        JobApplication app = new JobApplication();
        app.setId(APPLICATION_ID);
        app.setUserId(USER_ID);
        app.setCompany("Acme Corp");
        app.setJobTitle("Software Engineer");
        return app;
    }

    // Creates a fully populated targeting note for the given applicationId.
    private ResumeTargetingNote buildNote(Long id, Long applicationId) {
        ResumeTargetingNote note = new ResumeTargetingNote();
        note.setId(id);
        note.setApplicationId(applicationId);
        note.setMustHaveKeywords("Java, Spring Boot");
        note.setNiceToHaveKeywords("Kubernetes");
        note.setCustomBulletIdeas("Built a distributed system");
        note.setJobDescriptionExcerpt("Looking for a senior engineer");
        note.setMatchNotes("Strong match on backend skills");
        return note;
    }

    private TargetingNoteRequest buildRequest() {
        return new TargetingNoteRequest(
                "Java, Spring Boot",
                "Kubernetes",
                "Built a distributed system",
                "Looking for a senior engineer",
                "Strong match on backend skills"
        );
    }

    // -------------------------------------------------------------------------
    // get
    // -------------------------------------------------------------------------

    @Test
    void get_existingNote_returnsResponse() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(ownedApplication()));
        ResumeTargetingNote note = buildNote(NOTE_ID, APPLICATION_ID);
        when(noteRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Optional.of(note));

        TargetingNoteResponse response = targetingNoteService.get(USER_ID, APPLICATION_ID);

        assertThat(response.id()).isEqualTo(NOTE_ID);
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.mustHaveKeywords()).isEqualTo("Java, Spring Boot");
        assertThat(response.niceToHaveKeywords()).isEqualTo("Kubernetes");
    }

    @Test
    void get_noNote_throwsResourceNotFoundException() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(ownedApplication()));
        when(noteRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> targetingNoteService.get(USER_ID, APPLICATION_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Targeting note not found");
    }

    // -------------------------------------------------------------------------
    // upsert
    // -------------------------------------------------------------------------

    @Test
    void upsert_existingNote_updatesAndReturns() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(ownedApplication()));

        // An existing note is found — upsert should update it in place rather than create a new one.
        ResumeTargetingNote existing = buildNote(NOTE_ID, APPLICATION_ID);
        when(noteRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Optional.of(existing));

        ResumeTargetingNote afterSave = buildNote(NOTE_ID, APPLICATION_ID);
        afterSave.setMustHaveKeywords("Java, Spring Boot");
        when(noteRepository.save(existing)).thenReturn(afterSave);

        TargetingNoteRequest req = buildRequest();
        TargetingNoteResponse response = targetingNoteService.upsert(USER_ID, APPLICATION_ID, req);

        // The returned DTO reflects the updated values.
        assertThat(response.id()).isEqualTo(NOTE_ID);
        assertThat(response.mustHaveKeywords()).isEqualTo("Java, Spring Boot");

        // save was called with the existing entity (not a brand-new one).
        verify(noteRepository).save(existing);
    }

    @Test
    void upsert_noNote_createsAndReturns() {
        when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(ownedApplication()));

        // No existing note — the service must create a fresh entity.
        when(noteRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Optional.empty());

        ResumeTargetingNote freshlySaved = buildNote(NOTE_ID, APPLICATION_ID);
        when(noteRepository.save(any(ResumeTargetingNote.class))).thenReturn(freshlySaved);

        TargetingNoteRequest req = buildRequest();
        TargetingNoteResponse response = targetingNoteService.upsert(USER_ID, APPLICATION_ID, req);

        assertThat(response.id()).isEqualTo(NOTE_ID);
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);

        // Capture the entity passed to save and verify applicationId was set correctly.
        ArgumentCaptor<ResumeTargetingNote> captor = ArgumentCaptor.forClass(ResumeTargetingNote.class);
        verify(noteRepository).save(captor.capture());
        assertThat(captor.getValue().getApplicationId()).isEqualTo(APPLICATION_ID);
    }
}
