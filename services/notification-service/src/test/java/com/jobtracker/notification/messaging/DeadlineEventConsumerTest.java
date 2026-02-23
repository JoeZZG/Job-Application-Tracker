package com.jobtracker.notification.messaging;

import com.jobtracker.notification.entity.Notification;
import com.jobtracker.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadlineEventConsumerTest {

    @Mock
    private NotificationRepository notificationRepository;

    private DeadlineEventConsumer deadlineEventConsumer;

    @BeforeEach
    void setUp() {
        deadlineEventConsumer = new DeadlineEventConsumer(notificationRepository);
    }

    @Test
    void handleDeadlineEvent_savesNotification() {
        // Given
        LocalDate deadline = LocalDate.of(2026, 3, 15);
        DeadlineEventPayload payload = new DeadlineEventPayload(
                42L,           // applicationId
                7L,            // userId
                "Acme Corp",   // company
                "Software Engineer", // jobTitle
                deadline,      // deadline
                "DEADLINE_REMINDER", // eventType
                "2026-02-23T10:00:00Z" // timestamp
        );

        // Return the argument unchanged so the captor can inspect it
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        // When
        deadlineEventConsumer.handleDeadlineEvent(payload);

        // Then
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getType()).isEqualTo("DEADLINE_REMINDER");
        assertThat(saved.getTitle()).isEqualTo("Deadline approaching: Acme Corp");
        assertThat(saved.getMessage()).isEqualTo(
                "Your application for Software Engineer at Acme Corp has a deadline on 2026-03-15.");
        assertThat(saved.isRead()).isFalse();
    }
}
