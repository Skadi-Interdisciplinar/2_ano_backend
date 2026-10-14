package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.AlertHandlingResponse;
import skadi.api.enums.AlertHandlingStatus;
import skadi.api.exceptions.AlertHandlingNotFoundException;
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
    void findAll_returnsMappedHandlings() {
        AlertHandling handling = handling(10, AlertHandlingStatus.PENDENTE);
        when(repository.findAllByOrderByIdDesc()).thenReturn(List.of(handling));

        List<AlertHandlingResponse> result = service.findAll();

        assertEquals(1, result.size());
        assertResponse(result.get(0), handling);
        verify(repository).findAllByOrderByIdDesc();
    }

    @Test
    void findByAlertId_returnsSingleHandling() {
        AlertHandling handling = handling(10, AlertHandlingStatus.PENDENTE);
        when(repository.findFirstByAlertIdOrderByIdDesc(3)).thenReturn(Optional.of(handling));

        AlertHandlingResponse result = service.findByAlertId(3);

        assertResponse(result, handling);
        verify(repository).findFirstByAlertIdOrderByIdDesc(3);
    }

    @Test
    void findByAlertId_throwsWhenNoHandlingExists() {
        when(repository.findFirstByAlertIdOrderByIdDesc(3)).thenReturn(Optional.empty());

        assertThrows(AlertHandlingNotFoundException.class, () -> service.findByAlertId(3));
    }

    @Test
    void findById_returnsMappedHandling() {
        AlertHandling handling = handling(10, AlertHandlingStatus.EM_ANDAMENTO);
        when(repository.findById(10)).thenReturn(Optional.of(handling));

        AlertHandlingResponse result = service.findById(10);

        assertResponse(result, handling);
        verify(repository).findById(10);
    }

    @Test
    void findById_throwsWhenHandlingDoesNotExist() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(AlertHandlingNotFoundException.class, () -> service.findById(99));
        verify(repository).findById(99);
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
