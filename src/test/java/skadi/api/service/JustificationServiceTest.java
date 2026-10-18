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
import skadi.api.repository.JustificationRepository;

import java.time.LocalDateTime;
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
    @Mock private AuditUserContext auditUserContext;
    private JustificationService service;

    @BeforeEach
    void setUp() {
        service = new JustificationService(repository, auditUserContext);
    }

    @Test
    void findByAlertHandlingId_returnsMappedJustification() {
        Justification justification = justification(20, 5, "Power failure", "Temporary outage");
        when(repository.findByAlertHandlingId(5)).thenReturn(Optional.of(justification));

        JustificationResponse result = service.findByAlertHandlingId(5);

        assertResponse(result, justification);
        verify(repository).findByAlertHandlingId(5);
    }

    @Test
    void findByAlertHandlingId_throwsWhenNoneExists() {
        when(repository.findByAlertHandlingId(5)).thenReturn(Optional.empty());

        assertThrows(JustificationNotFoundException.class, () -> service.findByAlertHandlingId(5));
        verify(repository).findByAlertHandlingId(5);
    }

    @Test
    void create_savesJustificationForHandlingAndSetsTimestamp() {
        JustificationRequest request = new JustificationRequest("Power failure", "Temporary outage");
        when(repository.existsByAlertHandlingId(5)).thenReturn(false);
        when(repository.save(any(Justification.class))).thenAnswer(invocation -> {
            Justification saved = invocation.getArgument(0);
            saved.setId(20);
            return saved;
        });

        JustificationResponse result = service.create(5, request, 7);

        verify(auditUserContext).setCurrentUser(7);
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

        assertThrows(JustificationAlreadyExistsException.class,
                () -> service.create(5, new JustificationRequest("Motivo", "Descrição"), 7));
        verify(repository).existsByAlertHandlingId(5);
    }

    @Test
    void update_changesReasonAndDescription() {
        Justification justification = justification(20, 5, "Old reason", "Old description");
        when(repository.findByAlertHandlingId(5)).thenReturn(Optional.of(justification));
        when(repository.save(justification)).thenReturn(justification);

        JustificationResponse result = service.update(5,
                new JustificationPatchRequest("New reason", "New description"), 7);

        verify(auditUserContext).setCurrentUser(7);
        assertEquals("New reason", result.reason());
        assertEquals("New description", result.description());
        assertEquals(5, result.alertHandlingId());
        verify(repository).save(justification);
    }

    @Test
    void update_changesOnlyProvidedField() {
        Justification justification = justification(20, 5, "Old reason", "Keep this description");
        when(repository.findByAlertHandlingId(5)).thenReturn(Optional.of(justification));
        when(repository.save(justification)).thenReturn(justification);

        JustificationResponse result = service.update(5,
                new JustificationPatchRequest("New reason", null), 7);

        verify(auditUserContext).setCurrentUser(7);
        assertEquals("New reason", result.reason());
        assertEquals("Keep this description", result.description());
        verify(repository).save(justification);
    }

    @Test
    void update_throwsWhenNoFieldsAreProvided() {
        assertThrows(JustificationRuleViolationException.class,
                () -> service.update(5, new JustificationPatchRequest(null, null), 7));
    }

    @Test
    void update_throwsWhenJustificationDoesNotExistForHandling() {
        when(repository.findByAlertHandlingId(5)).thenReturn(Optional.empty());

        assertThrows(JustificationNotFoundException.class, () -> service.update(5,
                new JustificationPatchRequest("Reason", null), 7));
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
