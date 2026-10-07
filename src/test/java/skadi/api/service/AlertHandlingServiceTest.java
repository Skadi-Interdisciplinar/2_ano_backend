package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.AlertHandlingResponse;
import skadi.api.dto.AlertHandlingUpdateRequest;
import skadi.api.enums.AlertHandlingStatus;
import skadi.api.model.AlertHandling;
import skadi.api.repository.AlertHandlingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertHandlingServiceTest {
    @Mock private AlertHandlingRepository repository;
    private AlertHandlingService service;

    @BeforeEach
    void setUp() {
        service = new AlertHandlingService(repository);
    }

    @Test
    void findByAlertId_returnsMappedHandlings() {
        AlertHandling handling = handling(10, AlertHandlingStatus.PENDENTE);
        when(repository.findByAlertIdOrderByIdDesc(3)).thenReturn(List.of(handling));

        List<AlertHandlingResponse> result = service.findByAlertId(3);

        assertEquals(1, result.size());
        assertResponse(result.get(0), handling);
        verify(repository).findByAlertIdOrderByIdDesc(3);
    }

    @Test
    void findByAlertId_returnsEmptyListWhenAlertHasNoHandlings() {
        when(repository.findByAlertIdOrderByIdDesc(3)).thenReturn(List.of());

        List<AlertHandlingResponse> result = service.findByAlertId(3);

        assertEquals(List.of(), result);
        verify(repository).findByAlertIdOrderByIdDesc(3);
    }

    @Test
    void findById_returnsMappedHandling() {
        AlertHandling handling = handling(10, AlertHandlingStatus.EM_ANDAMENTO);
        when(repository.findByIdAndAlertId(10, 3)).thenReturn(Optional.of(handling));

        AlertHandlingResponse result = service.findById(3, 10);

        assertResponse(result, handling);
        verify(repository).findByIdAndAlertId(10, 3);
    }

    @Test
    void findById_throwsWhenHandlingDoesNotExist() {
        when(repository.findByIdAndAlertId(99, 3)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findById(3, 99));
        verify(repository).findByIdAndAlertId(99, 3);
    }

    @Test
    void update_changesStatusAndReturnsUpdatedHandling() {
        AlertHandling handling = handling(10, AlertHandlingStatus.PENDENTE);
        when(repository.findByIdAndAlertId(10, 3)).thenReturn(Optional.of(handling));
        when(repository.save(handling)).thenReturn(handling);

        AlertHandlingResponse result = service.update(3, 10,
                new AlertHandlingUpdateRequest(AlertHandlingStatus.EM_ANDAMENTO));

        assertEquals(AlertHandlingStatus.EM_ANDAMENTO, handling.getStatus());
        assertEquals(AlertHandlingStatus.EM_ANDAMENTO, result.status());
        verify(repository).save(handling);
    }

    @Test
    void update_throwsWhenHandlingDoesNotExist() {
        when(repository.findByIdAndAlertId(99, 3)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.update(3, 99,
                new AlertHandlingUpdateRequest(AlertHandlingStatus.EM_ANDAMENTO)));
    }

    @Test
    void delete_removesExistingHandling() {
        AlertHandling handling = handling(10, AlertHandlingStatus.PENDENTE);
        when(repository.findByIdAndAlertId(10, 3)).thenReturn(Optional.of(handling));

        service.delete(3, 10);

        verify(repository).findByIdAndAlertId(10, 3);
        verify(repository).delete(handling);
    }

    @Test
    void delete_throwsWhenHandlingDoesNotExist() {
        when(repository.findByIdAndAlertId(99, 3)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.delete(3, 99));
        verify(repository).findByIdAndAlertId(99, 3);
    }

    @Test
    void findById_throwsWhenHandlingBelongsToAnotherAlert() {
        when(repository.findByIdAndAlertId(10, 4)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findById(4, 10));
    }

    @Test
    void update_throwsWhenHandlingBelongsToAnotherAlert() {
        when(repository.findByIdAndAlertId(10, 4)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.update(4, 10,
                new AlertHandlingUpdateRequest(AlertHandlingStatus.EM_ANDAMENTO)));
    }

    @Test
    void delete_throwsWhenHandlingBelongsToAnotherAlert() {
        when(repository.findByIdAndAlertId(10, 4)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.delete(4, 10));
    }

    private AlertHandling handling(Integer id, AlertHandlingStatus status) {
        return new AlertHandling(id, 3, 7L,
                LocalDateTime.of(2026, 10, 5, 10, 0),
                LocalDateTime.of(2026, 10, 5, 11, 0), status);
    }

    private void assertResponse(AlertHandlingResponse response, AlertHandling handling) {
        assertEquals(handling.getId(), response.id());
        assertEquals(handling.getAlertId(), response.alertId());
        assertEquals(handling.getUserId(), response.userId());
        assertEquals(handling.getAcknowledgedAt(), response.acknowledgedAt());
        assertEquals(handling.getResolvedAt(), response.resolvedAt());
        assertEquals(handling.getStatus(), response.status());
    }
}
