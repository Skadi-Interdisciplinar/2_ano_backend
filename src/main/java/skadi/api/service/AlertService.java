package skadi.api.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import skadi.api.dto.AlertResponse;
import skadi.api.enums.AlertStatus;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.AlertNotFoundException;
import skadi.api.model.Alert;
import skadi.api.model.User;
import skadi.api.repository.AlertRepository;

import java.util.List;
import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository repository;
    private final DistributionCenterAccessService accessService;
    private final AuditUserContext auditUserContext;
    private final EntityManager entityManager;

    public List<AlertResponse> findAll(Integer coldRoomId, User user) {
        List<Alert> alerts = coldRoomId == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByColdRoomIdOrderByCreatedAtDesc(coldRoomId);

        return alerts.stream()
                .filter(alert -> accessService.canRead(
                        user, distributionCenterId(alert.getId())))
                .map(this::toResponse)
                .toList();
    }

    public AlertResponse findById(Integer id, User user) {
        Alert alert = repository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException(id));
        accessService.requireRead(user, distributionCenterId(id));
        return toResponse(alert);
    }

    public List<AlertResponse> findHistory(Integer coldRoomId, User user) {
        List<Alert> alerts = coldRoomId == null
                ? repository.findByStatusOrderByCreatedAtDesc(AlertStatus.RESOLVIDO)
                : repository.findByColdRoomIdAndStatusOrderByCreatedAtDesc(
                        coldRoomId, AlertStatus.RESOLVIDO);

        return alerts.stream()
                .filter(alert -> accessService.canRead(
                        user, distributionCenterId(alert.getId())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void recognize(Integer alertId, User user) {
        repository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        accessService.requireOperationalWrite(user, distributionCenterId(alertId));

        Integer userId = Math.toIntExact(user.getId());
        auditUserContext.setCurrentUser(user);

        entityManager.createNativeQuery("CALL sp_reconhecer_alerta(:alertId, :userId)")
                .setParameter("alertId", alertId)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    private Long distributionCenterId(Integer alertId) {
        return repository.findDistributionCenterIdByAlertId(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getColdRoomId(),
                alert.getReferenceShelfLifeHours(),
                alert.getCurrentLevel(),
                alert.getCreatedAt(),
                alert.getType(),
                alert.getSeverityLevel(),
                alert.getStatus()
        );
    }
}
