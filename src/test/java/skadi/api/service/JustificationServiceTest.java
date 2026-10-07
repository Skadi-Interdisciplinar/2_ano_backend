package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.JustificationRequest;
import skadi.api.dto.JustificationResponse;
import skadi.api.model.Justification;
import skadi.api.repository.JustificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JustificationServiceTest {
    @Mock private JustificationRepository repository;
    private JustificationService service;

    @BeforeEach
    void setUp() {
        service = new JustificationService(repository);
    }

    @Test
    void findByAlertHandlingId_returnsMappedJustifications() {
        Justification justification = justification(20, 5, "Power failure", "Temporary outage");
        when(repository.findByAlertHandlingIdOrderByCreatedAtDesc(5))
                .thenReturn(List.of(justification));

        List<JustificationResponse> result = service.findByAlertHandlingId(5);

        assertEquals(1, result.size());
        assertResponse(result.get(0), justification);
        verify(repository).findByAlertHandlingIdOrderByCreatedAtDesc(5);
    }

    @Test
    void findByAlertHandlingId_returnsEmptyListWhenNoneExist() {
        when(repository.findByAlertHandlingIdOrderByCreatedAtDesc(5)).thenReturn(List.of());

        assertEquals(List.of(), service.findByAlertHandlingId(5));
        verify(repository).findByAlertHandlingIdOrderByCreatedAtDesc(5);
    }

    @Test
    void create_savesJustificationForHandlingAndSetsTimestamp() {
        JustificationRequest request = new JustificationRequest("Power failure", "Temporary outage");
        when(repository.save(any(Justification.class))).thenAnswer(invocation -> {
            Justification saved = invocation.getArgument(0);
            saved.setId(20);
            return saved;
        });

        JustificationResponse result = service.create(5, request);

        ArgumentCaptor<Justification> captor = ArgumentCaptor.forClass(Justification.class);
        verify(repository).save(captor.capture());
        assertEquals(5, captor.getValue().getAlertHandlingId());
        assertEquals("Power failure", captor.getValue().getReason());
        assertEquals("Temporary outage", captor.getValue().getDescription());
        assertNotNull(captor.getValue().getCreatedAt());
        assertResponse(result, captor.getValue());
    }

    @Test
    void findById_returnsJustificationBelongingToHandling() {
        Justification justification = justification(20, 5, "Power failure", "Temporary outage");
        when(repository.findByIdAndAlertHandlingId(20, 5)).thenReturn(Optional.of(justification));

        JustificationResponse result = service.findById(5, 20);

        assertResponse(result, justification);
        verify(repository).findByIdAndAlertHandlingId(20, 5);
    }

    @Test
    void findById_throwsWhenJustificationDoesNotExistForHandling() {
        when(repository.findByIdAndAlertHandlingId(99, 5)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findById(5, 99));
    }

    @Test
    void findById_throwsWhenJustificationBelongsToAnotherHandling() {
        when(repository.findByIdAndAlertHandlingId(20, 6)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findById(6, 20));
    }

    @Test
    void update_changesReasonAndDescription() {
        Justification justification = justification(20, 5, "Old reason", "Old description");
        when(repository.findByIdAndAlertHandlingId(20, 5)).thenReturn(Optional.of(justification));
        when(repository.save(justification)).thenReturn(justification);

        JustificationResponse result = service.update(5, 20,
                new JustificationRequest("New reason", "New description"));

        assertEquals("New reason", result.reason());
        assertEquals("New description", result.description());
        assertEquals(5, result.alertHandlingId());
        verify(repository).save(justification);
    }

    @Test
    void update_throwsWhenJustificationDoesNotExistForHandling() {
        when(repository.findByIdAndAlertHandlingId(99, 5)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.update(5, 99,
                new JustificationRequest("Reason", "Description")));
    }

    @Test
    void update_throwsWhenJustificationBelongsToAnotherHandling() {
        when(repository.findByIdAndAlertHandlingId(20, 6)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.update(6, 20,
                new JustificationRequest("Reason", "Description")));
    }

    @Test
    void delete_removesJustificationBelongingToHandling() {
        Justification justification = justification(20, 5, "Power failure", "Temporary outage");
        when(repository.findByIdAndAlertHandlingId(20, 5)).thenReturn(Optional.of(justification));

        service.delete(5, 20);

        verify(repository).delete(justification);
    }

    @Test
    void delete_throwsWhenJustificationDoesNotExistForHandling() {
        when(repository.findByIdAndAlertHandlingId(99, 5)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.delete(5, 99));
    }

    @Test
    void delete_throwsWhenJustificationBelongsToAnotherHandling() {
        when(repository.findByIdAndAlertHandlingId(20, 6)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.delete(6, 20));
    }

    private Justification justification(Integer id, Integer handlingId, String reason, String description) {
        return new Justification(id, handlingId, reason, description,
                LocalDateTime.of(2026, 10, 7, 10, 0));
    }

    private void assertResponse(JustificationResponse response, Justification justification) {
        assertEquals(justification.getId(), response.id());
        assertEquals(justification.getAlertHandlingId(), response.alertHandlingId());
        assertEquals(justification.getReason(), response.reason());
        assertEquals(justification.getDescription(), response.description());
        assertEquals(justification.getCreatedAt(), response.createdAt());
    }
}
