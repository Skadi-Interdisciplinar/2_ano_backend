package skadi.api.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.AlertResponse;
import skadi.api.dto.AlertUpdateRequest;
import skadi.api.enums.AlertStatus;
import skadi.api.enums.CurrentLevel;
import skadi.api.enums.SeverityLevel;
import skadi.api.model.Alert;
import skadi.api.repository.AlertRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {
    @Mock private AlertRepository repository;
    @Mock private EntityManager entityManager;
    @Mock private Query query;
    private AlertService service;

    @BeforeEach
    void setUp() {
        service = new AlertService(repository, entityManager);
    }

    @Test
    void findAll_returnsAllAlerts() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(alert(AlertStatus.ATIVO)));

        List<AlertResponse> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals(AlertStatus.ATIVO, result.get(0).status());
        verify(repository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void findAll_filtersByColdRoom() {
        when(repository.findByColdRoomIdOrderByCreatedAtDesc(3))
                .thenReturn(List.of(alert(AlertStatus.RECONHECIDO)));

        List<AlertResponse> result = service.findAll(3);

        assertEquals(3, result.get(0).coldRoomId());
        verify(repository).findByColdRoomIdOrderByCreatedAtDesc(3);
    }

    @Test
    void findHistory_returnsOnlyResolvedAlerts() {
        when(repository.findByStatusOrderByCreatedAtDesc(AlertStatus.RESOLVIDO))
                .thenReturn(List.of(alert(AlertStatus.RESOLVIDO)));

        List<AlertResponse> result = service.findHistory();

        assertEquals(AlertStatus.RESOLVIDO, result.get(0).status());
        verify(repository).findByStatusOrderByCreatedAtDesc(AlertStatus.RESOLVIDO);
    }

    @Test
    void findHistory_filtersByColdRoom() {
        when(repository.findByColdRoomIdAndStatusOrderByCreatedAtDesc(3, AlertStatus.RESOLVIDO))
                .thenReturn(List.of(alert(AlertStatus.RESOLVIDO)));

        service.findHistory(3);

        verify(repository).findByColdRoomIdAndStatusOrderByCreatedAtDesc(3, AlertStatus.RESOLVIDO);
    }

    @Test
    void recognize_callsDatabaseProcedure() {
        when(entityManager.createNativeQuery("CALL sp_reconhecer_alerta(:alertId, :userId)"))
                .thenReturn(query);
        when(query.setParameter("alertId", 1)).thenReturn(query);
        when(query.setParameter("userId", 7)).thenReturn(query);

        service.recognize(1, 7);

        verify(query).executeUpdate();
    }

    @Test
    void update_keepsLevelSeverityAndStatusControlledByDatabase() {
        Alert current = alert(AlertStatus.ATIVO);
        when(repository.findById(1)).thenReturn(Optional.of(current));
        when(repository.save(current)).thenReturn(current);

        AlertUpdateRequest request = new AlertUpdateRequest(
                4, new BigDecimal("24.00"), "temperatura_fora_padrao");

        AlertResponse result = service.update(1, request);

        assertEquals(AlertStatus.ATIVO, result.status());
        assertEquals(4, current.getColdRoomId());
        assertEquals(CurrentLevel.OPERADOR, result.currentLevel());
        assertEquals(SeverityLevel.BAIXA, result.severityLevel());
        verify(repository).save(current);
    }

    @Test
    void findById_throwsWhenAlertDoesNotExist() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findById(99));
    }

    @Test
    void update_throwsWhenAlertDoesNotExist() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        AlertUpdateRequest request = new AlertUpdateRequest(
                3, new BigDecimal("24.00"), "temperatura_fora_padrao");

        assertThrows(IllegalArgumentException.class, () -> service.update(99, request));
    }

    @Test
    void delete_removesExistingAlert() {
        when(repository.existsById(1)).thenReturn(true);

        service.delete(1);

        verify(repository).deleteById(1);
    }

    @Test
    void delete_throwsWhenAlertDoesNotExist() {
        when(repository.existsById(99)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.delete(99));
    }

    private Alert alert(AlertStatus status) {
        return new Alert(1, 3, new BigDecimal("24.00"), CurrentLevel.OPERADOR,
                LocalDateTime.of(2026, 10, 5, 10, 0),
                "temperatura_fora_padrao", SeverityLevel.BAIXA, status);
    }
}
