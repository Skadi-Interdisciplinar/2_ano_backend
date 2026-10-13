package skadi.api.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skadi.api.dto.JustificationRequest;
import skadi.api.dto.JustificationPatchRequest;
import skadi.api.dto.JustificationResponse;
import skadi.api.exceptions.JustificationAlreadyExistsException;
import skadi.api.exceptions.JustificationNotFoundException;
import skadi.api.exceptions.JustificationRuleViolationException;
import skadi.api.mapper.JustificationMapper;
import skadi.api.model.Justification;
import skadi.api.repository.JustificationRepository;

import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class JustificationService {
    private final JustificationRepository repository;
    private final EntityManager entityManager;

    public JustificationResponse findByAlertHandlingId(Integer alertHandlingId) {
        return repository.findByAlertHandlingId(alertHandlingId)
                .map(JustificationMapper::toDTO)
                .orElseThrow(() -> new JustificationNotFoundException(alertHandlingId));
    }

    @Transactional
    public JustificationResponse create(
            Integer alertHandlingId,
            JustificationRequest request,
            Integer userId
    ) {
        if (repository.existsByAlertHandlingId(alertHandlingId)) {
            throw new JustificationAlreadyExistsException(alertHandlingId);
        }
        Justification justification = JustificationMapper.fromDTO(request, alertHandlingId);
        justification.setCreatedAt(LocalDateTime.now());
        setAuditUser(userId);
        return JustificationMapper.toDTO(repository.save(justification));
    }

    @Transactional
    public JustificationResponse update(
            Integer alertHandlingId,
            JustificationPatchRequest request,
            Integer userId
    ) {
        if (request.reason() == null && request.description() == null) {
            throw new JustificationRuleViolationException("Informe ao menos um campo para atualizar.");
        }

        Justification justification = repository.findByAlertHandlingId(alertHandlingId)
                .orElseThrow(() -> new JustificationNotFoundException(alertHandlingId));
        JustificationMapper.updateFromDTO(justification, request);
        setAuditUser(userId);
        return JustificationMapper.toDTO(repository.save(justification));
    }

    private void setAuditUser(Integer userId) {
        entityManager.createNativeQuery("SELECT set_config('app.usuario_atual', :userId, true)")
                .setParameter("userId", userId.toString())
                .getSingleResult();
    }

}
