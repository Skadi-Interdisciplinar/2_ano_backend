package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.NotificationResponse;
import skadi.api.exceptions.NotificationNotFoundException;
import skadi.api.model.AlertNotification;
import skadi.api.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock private NotificationRepository repository;
    @Mock private AuditUserContext auditUserContext;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, auditUserContext);
    }

    @Test
    void findAllByUserId_returnsOnlyRepositoryResultsMappedToResponses() {
        AlertNotification notification = notification(8, 12L, null);
        when(repository.findAllByUserIdOrderBySentAtDesc(12L)).thenReturn(List.of(notification));

        List<NotificationResponse> result = service.findAllByUserId(12L);

        assertEquals(1, result.size());
        assertEquals(8, result.get(0).id());
        assertEquals(12L, result.get(0).userId());
        verify(repository).findAllByUserIdOrderBySentAtDesc(12L);
    }

    @Test
    void findById_returnsNotificationForRecipient() {
        AlertNotification notification = notification(8, 12L, null);
        when(repository.findByIdAndUserId(8, 12L)).thenReturn(Optional.of(notification));

        NotificationResponse result = service.findById(8, 12L);

        assertEquals(8, result.id());
        assertEquals(12L, result.userId());
        verify(repository).findByIdAndUserId(8, 12L);
    }

    @Test
    void findById_throwsNotFoundWhenNotificationDoesNotBelongToRecipient() {
        when(repository.findByIdAndUserId(8, 99L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> service.findById(8, 99L));
    }

    @Test
    void markAsRead_setsTimestampAndSavesUnreadNotification() {
        AlertNotification notification = notification(8, 12L, null);
        when(repository.findByIdAndUserId(8, 12L)).thenReturn(Optional.of(notification));
        when(repository.save(notification)).thenReturn(notification);

        NotificationResponse result = service.markAsRead(8, 12L);

        verify(auditUserContext).setCurrentUser(12L);
        assertNotNull(result.readAt());
        assertEquals(notification.getReadAt(), result.readAt());
        verify(auditUserContext).setCurrentUser(12L);
        verify(repository).save(notification);
    }

    @Test
    void markAsRead_preservesFirstReadTimestampWhenAlreadyRead() {
        LocalDateTime firstReadAt = LocalDateTime.of(2026, 10, 16, 10, 30);
        AlertNotification notification = notification(8, 12L, firstReadAt);
        when(repository.findByIdAndUserId(8, 12L)).thenReturn(Optional.of(notification));

        NotificationResponse result = service.markAsRead(8, 12L);

        assertEquals(firstReadAt, result.readAt());
        verify(repository, never()).save(any(AlertNotification.class));
    }

    @Test
    void markAsRead_throwsNotFoundWhenNotificationDoesNotBelongToRecipient() {
        when(repository.findByIdAndUserId(8, 99L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> service.markAsRead(8, 99L));
        verify(repository, never()).save(any(AlertNotification.class));
    }

    private AlertNotification notification(Integer id, Long userId, LocalDateTime readAt) {
        return new AlertNotification(
                id,
                22,
                userId,
                LocalDateTime.of(2026, 10, 16, 10, 0),
                readAt
        );
    }
}
