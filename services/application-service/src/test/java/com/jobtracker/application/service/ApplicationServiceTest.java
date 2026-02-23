package com.jobtracker.application.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private ResumeTargetingNoteRepository targetingNoteRepository;

    @Mock
    private DeadlineEventPublisher deadlineEventPublisher;

    @InjectMocks
    private ApplicationService applicationService;

    private static final Long USER_ID = 1L;
    private static final Long APP_ID = 10L;
    private static final Long OTHER_USER_ID = 99L;

    // Builds a fully populated entity that simulates what the repository returns after save.
    private JobApplication buildApplication(Long id, Long userId, LocalDate deadline) {
        JobApplication app = new JobApplication();
        app.setId(id);
        app.setUserId(userId);
        app.setCompany("Acme Corp");
        app.setJobTitle("Software Engineer");
        app.setLocation("Remote");
        app.setJobPostUrl("https://example.com/job/1");
        app.setDeadline(deadline);
        app.setAppliedDate(LocalDate.of(2026, 1, 15));
        app.setStatus(ApplicationStatus.APPLIED);
        app.setNotes("Some notes");
        return app;
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_withDeadline_savesAndPublishesEvent() {
        LocalDate deadline = LocalDate.of(2026, 3, 1);
        CreateApplicationRequest req = new CreateApplicationRequest(
                "Acme Corp", "Software Engineer", "Remote",
                "https://example.com/job/1", deadline,
                LocalDate.of(2026, 1, 15), ApplicationStatus.APPLIED, "notes");

        JobApplication saved = buildApplication(APP_ID, USER_ID, deadline);
        when(applicationRepository.save(any(JobApplication.class))).thenReturn(saved);

        ApplicationResponse response = applicationService.create(USER_ID, req);

        assertThat(response.id()).isEqualTo(APP_ID);
        assertThat(response.company()).isEqualTo("Acme Corp");
        assertThat(response.deadline()).isEqualTo(deadline);

        // A deadline event must be published because deadline is non-null.
        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        verify(deadlineEventPublisher).publishDeadlineEvent(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(APP_ID);
    }

    @Test
    void create_withoutDeadline_doesNotPublishEvent() {
        CreateApplicationRequest req = new CreateApplicationRequest(
                "Acme Corp", "Software Engineer", null, null,
                null, null, null, null);

        JobApplication saved = buildApplication(APP_ID, USER_ID, null);
        when(applicationRepository.save(any(JobApplication.class))).thenReturn(saved);

        applicationService.create(USER_ID, req);

        // Deadline is null on the saved entity — no event should be sent.
        verify(deadlineEventPublisher, never()).publishDeadlineEvent(any());
    }

    // -------------------------------------------------------------------------
    // list
    // -------------------------------------------------------------------------

    @Test
    void list_noStatusFilter_returnsAll() {
        List<JobApplication> apps = List.of(
                buildApplication(10L, USER_ID, null),
                buildApplication(11L, USER_ID, null));
        when(applicationRepository.findByUserId(USER_ID)).thenReturn(apps);

        List<ApplicationResponse> result = applicationService.list(USER_ID, null);

        assertThat(result).hasSize(2);
        verify(applicationRepository).findByUserId(USER_ID);
        verify(applicationRepository, never()).findByUserIdAndStatus(any(), any());
    }

    @Test
    void list_withStatusFilter_returnsFiltered() {
        List<JobApplication> filtered = List.of(buildApplication(10L, USER_ID, null));
        when(applicationRepository.findByUserIdAndStatus(USER_ID, ApplicationStatus.APPLIED))
                .thenReturn(filtered);

        List<ApplicationResponse> result = applicationService.list(USER_ID, ApplicationStatus.APPLIED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ApplicationStatus.APPLIED);
        verify(applicationRepository).findByUserIdAndStatus(USER_ID, ApplicationStatus.APPLIED);
        verify(applicationRepository, never()).findByUserId(any());
    }

    // -------------------------------------------------------------------------
    // getById
    // -------------------------------------------------------------------------

    @Test
    void getById_ownerAccess_returnsResponse() {
        JobApplication app = buildApplication(APP_ID, USER_ID, null);
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));

        ApplicationResponse response = applicationService.getById(USER_ID, APP_ID);

        assertThat(response.id()).isEqualTo(APP_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getById(USER_ID, APP_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    void getById_differentOwner_throwsForbiddenException() {
        // The application belongs to a different user.
        JobApplication app = buildApplication(APP_ID, OTHER_USER_ID, null);
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.getById(USER_ID, APP_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not own this application");
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void update_changesDeadline_publishesEvent() {
        LocalDate originalDeadline = LocalDate.of(2026, 2, 1);
        LocalDate newDeadline = LocalDate.of(2026, 4, 1);

        JobApplication existing = buildApplication(APP_ID, USER_ID, originalDeadline);
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(existing));

        // The save stub returns the entity as it looks after the update.
        JobApplication afterSave = buildApplication(APP_ID, USER_ID, newDeadline);
        when(applicationRepository.save(existing)).thenReturn(afterSave);

        UpdateApplicationRequest req = new UpdateApplicationRequest(
                null, null, null, null, newDeadline, null, null, null);

        ApplicationResponse response = applicationService.update(USER_ID, APP_ID, req);

        assertThat(response.deadline()).isEqualTo(newDeadline);

        // Deadline changed (was originalDeadline, now newDeadline) — event must fire.
        verify(deadlineEventPublisher).publishDeadlineEvent(afterSave);
    }

    @Test
    void update_sameDeadline_doesNotPublishEvent() {
        LocalDate deadline = LocalDate.of(2026, 2, 1);

        JobApplication existing = buildApplication(APP_ID, USER_ID, deadline);
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(existing));
        when(applicationRepository.save(existing)).thenReturn(existing);

        // The request sends the same deadline value — no actual change.
        UpdateApplicationRequest req = new UpdateApplicationRequest(
                null, null, null, null, deadline, null, null, null);

        applicationService.update(USER_ID, APP_ID, req);

        // Deadline equals the previous value, so no event should be published.
        verify(deadlineEventPublisher, never()).publishDeadlineEvent(any());
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_ownerAccess_deletesApplication() {
        JobApplication app = buildApplication(APP_ID, USER_ID, null);
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));

        applicationService.delete(USER_ID, APP_ID);

        verify(applicationRepository).delete(app);
    }

    @Test
    void delete_differentOwner_throwsForbiddenException() {
        JobApplication app = buildApplication(APP_ID, OTHER_USER_ID, null);
        when(applicationRepository.findById(APP_ID)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.delete(USER_ID, APP_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You do not own this application");

        verify(applicationRepository, never()).delete(any());
    }
}
