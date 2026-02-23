package com.jobtracker.notification.service;

import com.jobtracker.notification.dto.NotificationResponse;
import com.jobtracker.notification.entity.Notification;
import com.jobtracker.notification.exception.ForbiddenException;
import com.jobtracker.notification.exception.ResourceNotFoundException;
import com.jobtracker.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    // ---------------------------------------------------------------------------
    // listForUser
    // ---------------------------------------------------------------------------

    @Test
    void list_returnsNotificationsForUser() {
        // Given
        Long userId = 1L;
        Notification first = buildNotification(10L, userId, "DEADLINE_REMINDER",
                "Deadline approaching: Acme", "Your application for SWE at Acme has a deadline on 2026-03-01.",
                false, LocalDateTime.of(2026, 2, 20, 9, 0));
        Notification second = buildNotification(11L, userId, "DEADLINE_REMINDER",
                "Deadline approaching: Beta Corp", "Your application for SRE at Beta Corp has a deadline on 2026-03-05.",
                true, LocalDateTime.of(2026, 2, 18, 8, 0));

        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .willReturn(List.of(first, second));

        // When
        List<NotificationResponse> result = notificationService.listForUser(userId);

        // Then
        assertThat(result).hasSize(2);

        NotificationResponse firstResponse = result.get(0);
        assertThat(firstResponse.id()).isEqualTo(10L);
        assertThat(firstResponse.userId()).isEqualTo(userId);
        assertThat(firstResponse.type()).isEqualTo("DEADLINE_REMINDER");
        assertThat(firstResponse.title()).isEqualTo("Deadline approaching: Acme");
        assertThat(firstResponse.message()).isEqualTo("Your application for SWE at Acme has a deadline on 2026-03-01.");
        assertThat(firstResponse.isRead()).isFalse();
        assertThat(firstResponse.createdAt()).isEqualTo(LocalDateTime.of(2026, 2, 20, 9, 0));

        NotificationResponse secondResponse = result.get(1);
        assertThat(secondResponse.id()).isEqualTo(11L);
        assertThat(secondResponse.isRead()).isTrue();

        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void list_returnsEmptyList_whenUserHasNoNotifications() {
        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(99L))
                .willReturn(List.of());

        List<NotificationResponse> result = notificationService.listForUser(99L);

        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------------------
    // markAsRead — happy path
    // ---------------------------------------------------------------------------

    @Test
    void markRead_ownerAccess_setsIsReadTrue() {
        // Given
        Long userId = 1L;
        Long notificationId = 10L;
        Notification notification = buildNotification(notificationId, userId, "DEADLINE_REMINDER",
                "Deadline approaching: Acme", "Some message.", false,
                LocalDateTime.of(2026, 2, 20, 9, 0));

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        ArgumentCaptor<Notification> savedCaptor = ArgumentCaptor.forClass(Notification.class);
        given(notificationRepository.save(savedCaptor.capture())).willReturn(notification);

        // When
        NotificationResponse response = notificationService.markAsRead(userId, notificationId);

        // Then — the entity was mutated before save
        Notification saved = savedCaptor.getValue();
        assertThat(saved.isRead()).isTrue();
        assertThat(saved.getId()).isEqualTo(notificationId);
        assertThat(saved.getUserId()).isEqualTo(userId);

        // And the returned DTO reflects the updated state
        assertThat(response.isRead()).isTrue();
        assertThat(response.id()).isEqualTo(notificationId);
        assertThat(response.userId()).isEqualTo(userId);

        verify(notificationRepository).findById(notificationId);
        verify(notificationRepository).save(notification);
    }

    // ---------------------------------------------------------------------------
    // markAsRead — not found
    // ---------------------------------------------------------------------------

    @Test
    void markRead_notFound_throwsResourceNotFoundException() {
        Long notificationId = 999L;
        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, notificationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(notificationId));
    }

    // ---------------------------------------------------------------------------
    // markAsRead — wrong owner
    // ---------------------------------------------------------------------------

    @Test
    void markRead_differentOwner_throwsForbiddenException() {
        Long ownerId = 1L;
        Long callerId = 2L;
        Long notificationId = 10L;
        Notification notification = buildNotification(notificationId, ownerId, "DEADLINE_REMINDER",
                "Deadline approaching: Acme", "Some message.", false,
                LocalDateTime.of(2026, 2, 20, 9, 0));

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(callerId, notificationId))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private Notification buildNotification(Long id, Long userId, String type,
                                           String title, String message,
                                           boolean isRead, LocalDateTime createdAt) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setRead(isRead);
        n.setCreatedAt(createdAt);
        return n;
    }
}
