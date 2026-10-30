package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.JustificationRequest;
import skadi.api.dto.JustificationPatchRequest;
import skadi.api.dto.JustificationResponse;
import skadi.api.exceptions.JustificationAlreadyExistsException;
import skadi.api.exceptions.JustificationNotFoundException;
import skadi.api.exceptions.JustificationRuleViolationException;
import skadi.api.model.Justification;
import skadi.api.model.AlertHandling;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;
import skadi.api.enums.AlertHandlingStatus;
import skadi.api.enums.NivelAcesso;
import skadi.api.repository.AlertHandlingRepository;
import skadi.api.repository.AlertRepository;
import skadi.api.repository.JustificationRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JustificationServiceTest {
    @Mock private JustificationRepository repository;
    @Mock private AuditUserContext auditUserContext;
    @Mock private AlertHandlingRepository alertHandlingRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private DistributionCenterAccessService accessService;
    private JustificationService service;

    @BeforeEach
    void setUp() {
        service = new JustificationService(repository, auditUserContext,
                alertHandlingRepository, alertRepository, accessService);
    }

    @Test
    void findByAlertHandlingId_returnsMappedJustification() {
        Justification justification = justification(20, 5, "Power failure", "Temporary outage");
        allowHandling(5);
        when(repository.findByAlertHandlingId(5)).thenReturn(Optional.of(justification));

        JustificationResponse result = service.findByAlertHandlingId(5, user());

        assertResponse(result, justification);
        verify(repository).findByAlertHandlingId(5);
    }

    @Test
    void findByAlertHandlingId_throwsWhenNoneExists() {
        allowHandling(5);
        when(repository.findByAlertHandlingId(5)).thenReturn(Optional.empty());

        assertThrows(JustificationNotFoundException.class,
                () -> service.findByAlertHandlingId(5, user()));
        verify(repository).findByAlertHandlingId(5);
    }

    @Test
    void create_savesJustificationForHandlingAndSetsTimestamp() {
        JustificationRequest request = new JustificationRequest("Power failure", "Temporary outage");
        allowHandling(5);
        when(repository.existsByAlertHandlingId(5)).thenReturn(false);
        when(repository.save(any(Justification.class))).thenAnswer(invocation -> {
            Justification saved = invocation.getArgument(0);
            saved.setId(20);
            return saved;
        });

        JustificationResponse result = service.create(5, request, user());

        verify(auditUserContext).setCurrentUser(eq(7L));
        ArgumentCaptor<Justification> captor = ArgumentCaptor.forClass(Justification.class);
        verify(repository).save(captor.capture());
        assertEquals(5, captor.getValue().getAlertHandlingId());
        assertEquals("Power failure", captor.getValue().getReason());
        assertEquals("Temporary outage", captor.getValue().getDescription());
        assertNotNull(captor.getValue().getCreatedAt());
        assertResponse(result, captor.getValue());
    }

    @Test
    void create_throwsWhenHandlingAlreadyHasJustification() {
        when(repository.existsByAlertHandlingId(5)).thenReturn(true);
        allowHandling(5);

        assertThrows(JustificationAlreadyExistsException.class,
                () -> service.create(5, new JustificationRequest("Motivo", "Descrição"), user()));
        verify(repository).existsByAlertHandlingId(5);
    }

    @Test
    void update_changesReasonAndDescription() {
        Justification justification = justification(20, 5, "Old reason", "Old description");
        allowHandling(5);
        when(repository.findByAlertHandlingId(5)).thenReturn(Optional.of(justification));
        when(repository.save(justification)).thenReturn(justification);

        JustificationResponse result = service.update(5,
                new JustificationPatchRequest("New reason", "New description"), user());

        verify(auditUserContext).setCurrentUser(eq(7L));
        assertEquals("New reason", result.reason());
        assertEquals("New description", result.description());
        assertEquals(5, result.alertHandlingId());
        verify(repository).save(justification);
    }

    @Test
    void update_changesOnlyProvidedField() {
        Justification justification = justification(20, 5, "Old reason", "Keep this description");
        allowHandling(5);
        when(repository.findByAlertHandlingId(5)).thenReturn(Optional.of(justification));
        when(repository.save(justification)).thenReturn(justification);

        JustificationResponse result = service.update(5,
                new JustificationPatchRequest("New reason", null), user());

        verify(auditUserContext).setCurrentUser(eq(7L));
        assertEquals("New reason", result.reason());
        assertEquals("Keep this description", result.description());
        verify(repository).save(justification);
    }

    @Test
    void update_throwsWhenNoFieldsAreProvided() {
        assertThrows(JustificationRuleViolationException.class,
                () -> service.update(5, new JustificationPatchRequest(null, null), user()));
    }

    @Test
    void update_throwsWhenJustificationDoesNotExistForHandling() {
        allowHandling(5);
        when(repository.findByAlertHandlingId(5)).thenReturn(Optional.empty());

        assertThrows(JustificationNotFoundException.class, () -> service.update(5,
                new JustificationPatchRequest("Reason", null), user()));
    }

    private Justification justification(Integer id, Integer handlingId, String reason, String description) {
        return new Justification(id, handlingId, reason, description,
                LocalDateTime.of(2026, 10, 7, 10, 0));
    }

    private void allowHandling(Integer handlingId) {
        when(alertHandlingRepository.findById(handlingId)).thenReturn(Optional.of(
                new AlertHandling(handlingId, 3, 7L, null, null, AlertHandlingStatus.PENDENTE)));
        when(alertRepository.findDistributionCenterIdByAlertId(3)).thenReturn(Optional.of(1L));
    }

    private User user() {
        return new User(7L, "Usuário", "usuario", "12345678901", "email@test.com",
                "senha", NivelAcesso.OPERADOR,
                new DistributionCenter(1L, "CD", "12345678901234", null), null, 8);
    }

    private void assertResponse(JustificationResponse response, Justification justification) {
        assertEquals(justification.getId(), response.id());
        assertEquals(justification.getAlertHandlingId(), response.alertHandlingId());
        assertEquals(justification.getReason(), response.reason());
        assertEquals(justification.getDescription(), response.description());
        assertEquals(justification.getCreatedAt(), response.createdAt());
    }
}
