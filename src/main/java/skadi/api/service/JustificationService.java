package skadi.api.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skadi.api.dto.JustificationRequest;
import skadi.api.dto.JustificationPatchRequest;
import skadi.api.dto.JustificationResponse;
import skadi.api.exceptions.JustificationAlreadyExistsException;
import skadi.api.exceptions.JustificationNotFoundException;
import skadi.api.exceptions.JustificationRuleViolationException;
import skadi.api.exceptions.AlertHandlingNotFoundException;
import skadi.api.mapper.JustificationMapper;
import skadi.api.model.Justification;
import skadi.api.model.AlertHandling;
import skadi.api.model.User;
import skadi.api.repository.AlertHandlingRepository;
import skadi.api.repository.AlertRepository;
import skadi.api.repository.JustificationRepository;

import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class JustificationService {
    private final JustificationRepository repository;
    private final AuditUserContext auditUserContext;
    private final AlertHandlingRepository alertHandlingRepository;
    private final AlertRepository alertRepository;
    private final DistributionCenterAccessService accessService;

    public JustificationResponse findByAlertHandlingId(Integer alertHandlingId, User user) {
        requireReadAccess(alertHandlingId, user);
        return repository.findByAlertHandlingId(alertHandlingId)
                .map(JustificationMapper::toDTO)
                .orElseThrow(() -> new JustificationNotFoundException(alertHandlingId));
    }

    @Transactional
    public JustificationResponse create(
            Integer alertHandlingId,
            JustificationRequest request,
            User user
    ) {
        requireWriteAccess(alertHandlingId, user);
        if (repository.existsByAlertHandlingId(alertHandlingId)) {
            throw new JustificationAlreadyExistsException(alertHandlingId);
        }
        Justification justification = JustificationMapper.fromDTO(request, alertHandlingId);
        justification.setCreatedAt(LocalDateTime.now());
        auditUserContext.setCurrentUser(user);
        return JustificationMapper.toDTO(repository.save(justification));
    }

    @Transactional
    public JustificationResponse update(
            Integer alertHandlingId,
            JustificationPatchRequest request,
            User user
    ) {
        if (request.reason() == null && request.description() == null) {
            throw new JustificationRuleViolationException("Informe ao menos um campo para atualizar.");
        }

        requireWriteAccess(alertHandlingId, user);
        Justification justification = repository.findByAlertHandlingId(alertHandlingId)
                .orElseThrow(() -> new JustificationNotFoundException(alertHandlingId));
        JustificationMapper.updateFromDTO(justification, request);
        auditUserContext.setCurrentUser(user);
        return JustificationMapper.toDTO(repository.save(justification));
    }

    private void requireReadAccess(Integer alertHandlingId, User user) {
        accessService.requireRead(user, distributionCenterId(alertHandlingId));
    }

    private void requireWriteAccess(Integer alertHandlingId, User user) {
        accessService.requireOperationalWrite(user, distributionCenterId(alertHandlingId));
    }

    private Long distributionCenterId(Integer alertHandlingId) {
        AlertHandling handling = alertHandlingRepository.findById(alertHandlingId)
                .orElseThrow(() -> AlertHandlingNotFoundException.forHandlingId(alertHandlingId));
        return alertRepository.findDistributionCenterIdByAlertId(handling.getAlertId())
                .orElseThrow(() -> AlertHandlingNotFoundException.forAlertId(handling.getAlertId()));
    }

}
