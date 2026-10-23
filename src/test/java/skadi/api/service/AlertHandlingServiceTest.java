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
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;
import skadi.api.enums.NivelAcesso;
import skadi.api.repository.AlertRepository;
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
    @Mock private AlertRepository alertRepository;
    @Mock private DistributionCenterAccessService accessService;
    private AlertHandlingService service;

    @BeforeEach
    void setUp() {
        service = new AlertHandlingService(repository, alertRepository, accessService);
    }

    @Test
    void findAll_returnsMappedHandlings() {
        AlertHandling handling = handling(10, AlertHandlingStatus.PENDENTE);
        User user = user();
        when(repository.findAllByOrderByIdDesc()).thenReturn(List.of(handling));
        when(alertRepository.findDistributionCenterIdByAlertId(3)).thenReturn(Optional.of(1L));
        when(accessService.canRead(user, 1L)).thenReturn(true);

        List<AlertHandlingResponse> result = service.findAll(user);

        assertEquals(1, result.size());
        assertResponse(result.get(0), handling);
        verify(repository).findAllByOrderByIdDesc();
    }

    @Test
    void findByAlertId_returnsSingleHandling() {
        AlertHandling handling = handling(10, AlertHandlingStatus.PENDENTE);
        when(repository.findFirstByAlertIdOrderByIdDesc(3)).thenReturn(Optional.of(handling));
        when(alertRepository.findDistributionCenterIdByAlertId(3)).thenReturn(Optional.of(1L));

        AlertHandlingResponse result = service.findByAlertId(3, user());

        assertResponse(result, handling);
        verify(repository).findFirstByAlertIdOrderByIdDesc(3);
    }

    @Test
    void findByAlertId_throwsWhenNoHandlingExists() {
        when(repository.findFirstByAlertIdOrderByIdDesc(3)).thenReturn(Optional.empty());

        assertThrows(AlertHandlingNotFoundException.class, () -> service.findByAlertId(3, user()));
    }

    @Test
    void findById_returnsMappedHandling() {
        AlertHandling handling = handling(10, AlertHandlingStatus.EM_ANDAMENTO);
        when(repository.findById(10)).thenReturn(Optional.of(handling));
        when(alertRepository.findDistributionCenterIdByAlertId(3)).thenReturn(Optional.of(1L));

        AlertHandlingResponse result = service.findById(10, user());

        assertResponse(result, handling);
        verify(repository).findById(10);
    }

    @Test
    void findById_throwsWhenHandlingDoesNotExist() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(AlertHandlingNotFoundException.class, () -> service.findById(99, user()));
        verify(repository).findById(99);
    }

    private AlertHandling handling(Integer id, AlertHandlingStatus status) {
        return new AlertHandling(id, 3, 7L,
                LocalDateTime.of(2026, 10, 5, 10, 0),
                LocalDateTime.of(2026, 10, 5, 11, 0), status);
    }

    private User user() {
        return new User(7L, "Usuário", "usuario", "12345678901", "email@test.com",
                "senha", NivelAcesso.OPERADOR,
                new DistributionCenter(1L, "CD", "12345678901234"), 8);
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
