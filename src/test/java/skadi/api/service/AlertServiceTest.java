package skadi.api.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import skadi.api.dto.AlertResponse;
import skadi.api.enums.AlertStatus;
import skadi.api.enums.CurrentLevel;
import skadi.api.enums.NivelAcesso;
import skadi.api.enums.SeverityLevel;
import skadi.api.exceptions.AlertNotFoundException;
import skadi.api.model.Alert;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;
import skadi.api.repository.AlertRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {
    @Mock private AlertRepository repository;
    @Mock private DistributionCenterAccessService accessService;
    @Mock private AuditUserContext auditUserContext;
    @Mock private EntityManager entityManager;
    @Mock private Query query;
    private AlertService service;

    @BeforeEach
    void setUp() {
        service = new AlertService(repository, accessService, auditUserContext, entityManager);
    }

    @Test
    void findAll_returnsAllAlerts() {
        User user = user(7L, NivelAcesso.ADMIN, null);
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(alert(AlertStatus.ATIVO)));
        allowRead(user, 1);

        List<AlertResponse> result = service.findAll(null, user);

        assertEquals(1, result.size());
        assertEquals(AlertStatus.ATIVO, result.get(0).status());
        verify(repository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void findAll_filtersByColdRoom() {
        User user = user(7L, NivelAcesso.GESTOR, 1L);
        when(repository.findByColdRoomIdOrderByCreatedAtDesc(3))
                .thenReturn(List.of(alert(AlertStatus.RECONHECIDO)));
        allowRead(user, 1);

        List<AlertResponse> result = service.findAll(3, user);

        assertEquals(3, result.get(0).coldRoomId());
        verify(repository).findByColdRoomIdOrderByCreatedAtDesc(3);
    }

    @Test
    void findHistory_returnsOnlyResolvedAlerts() {
        User user = user(7L, NivelAcesso.OPERADOR, 1L);
        when(repository.findByStatusOrderByCreatedAtDesc(AlertStatus.RESOLVIDO))
                .thenReturn(List.of(alert(AlertStatus.RESOLVIDO)));
        allowRead(user, 1);

        List<AlertResponse> result = service.findHistory(null, user);

        assertEquals(AlertStatus.RESOLVIDO, result.get(0).status());
        verify(repository).findByStatusOrderByCreatedAtDesc(AlertStatus.RESOLVIDO);
    }

    @Test
    void findHistory_filtersByColdRoom() {
        User user = user(7L, NivelAcesso.OPERADOR, 1L);
        when(repository.findByColdRoomIdAndStatusOrderByCreatedAtDesc(3, AlertStatus.RESOLVIDO))
                .thenReturn(List.of(alert(AlertStatus.RESOLVIDO)));
        allowRead(user, 1);

        service.findHistory(3, user);

        verify(repository).findByColdRoomIdAndStatusOrderByCreatedAtDesc(3, AlertStatus.RESOLVIDO);
    }

    @Test
    void recognize_callsDatabaseProcedure() {
        User user = user(7L, NivelAcesso.OPERADOR, 1L);
        when(repository.findById(1)).thenReturn(Optional.of(alert(AlertStatus.ATIVO)));
        when(repository.findDistributionCenterIdByAlertId(1)).thenReturn(Optional.of(1L));
        when(entityManager.createNativeQuery("CALL sp_reconhecer_alerta(:alertId, :userId)"))
                .thenReturn(query);
        when(query.setParameter("alertId", 1)).thenReturn(query);
        when(query.setParameter("userId", 7)).thenReturn(query);

        service.recognize(1, user);

        verify(auditUserContext).setCurrentUser(user);
        verify(query).executeUpdate();
    }

    @Test
    void recognize_deniesSuperAdmin() {
        User superAdmin = user(7L, NivelAcesso.SUPER_ADMIN, null);
        when(repository.findById(1)).thenReturn(Optional.of(alert(AlertStatus.ATIVO)));
        when(repository.findDistributionCenterIdByAlertId(1)).thenReturn(Optional.of(1L));
        doThrow(new AccessDeniedException("Sem permissão"))
                .when(accessService).requireOperationalWrite(superAdmin, 1L);

        assertThrows(AccessDeniedException.class, () -> service.recognize(1, superAdmin));
    }

    @Test
    void recognize_deniesUserFromDifferentDistributionCenter() {
        User user = user(7L, NivelAcesso.ADMIN, 2L);
        when(repository.findById(1)).thenReturn(Optional.of(alert(AlertStatus.ATIVO)));
        when(repository.findDistributionCenterIdByAlertId(1)).thenReturn(Optional.of(1L));
        doThrow(new AccessDeniedException("Sem permissão"))
                .when(accessService).requireOperationalWrite(user, 1L);

        assertThrows(AccessDeniedException.class, () -> service.recognize(1, user));
    }

    @Test
    void findById_throwsWhenAlertDoesNotExist() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class,
                () -> service.findById(99, user(7L, NivelAcesso.SUPER_ADMIN, null)));
    }

    private void allowRead(User user, Integer alertId) {
        when(repository.findDistributionCenterIdByAlertId(alertId)).thenReturn(Optional.of(1L));
        when(accessService.canRead(user, 1L)).thenReturn(true);
    }

    private Alert alert(AlertStatus status) {
        return new Alert(1, 3, new BigDecimal("24.00"), CurrentLevel.OPERADOR,
                LocalDateTime.of(2026, 10, 5, 10, 0),
                "temperatura_fora_padrao", SeverityLevel.BAIXA, status);
    }

    private User user(Long id, NivelAcesso nivelAcesso, Long cdId) {
        DistributionCenter cd = cdId == null ? null
                : new DistributionCenter(cdId, "CD de teste", "12345678901234", null);
        if (nivelAcesso == NivelAcesso.ADMIN) {
            return new User(id, "Usuário", "usuario", "12345678901", "email@test.com",
                    "senha", nivelAcesso, null, cd == null ? null : List.of(cd), null);
        }
        return new User(id, "Usuário", "usuario", "12345678901", "email@test.com",
                "senha", nivelAcesso, cd, null, null);
    }
}
