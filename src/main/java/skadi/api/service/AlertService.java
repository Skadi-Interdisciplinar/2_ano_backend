package skadi.api.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skadi.api.dto.AlertResponse;
import skadi.api.enums.AlertStatus;
import skadi.api.exceptions.AlertNotFoundException;
import skadi.api.model.Alert;
import skadi.api.repository.AlertRepository;

import java.util.List;
import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository repository;
    private final EntityManager entityManager;

    public List<AlertResponse> findAll() {
        return findAll(null);
    }

    public List<AlertResponse> findAll(Integer coldRoomId) {
        List<Alert> alerts = coldRoomId == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByColdRoomIdOrderByCreatedAtDesc(coldRoomId);

        return alerts.stream()
                .map(this::toResponse)
                .toList();
    }

    public AlertResponse findById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new AlertNotFoundException(id));
    }

    public List<AlertResponse> findHistory() {
        return findHistory(null);
    }

    public List<AlertResponse> findHistory(Integer coldRoomId) {
        List<Alert> alerts = coldRoomId == null
                ? repository.findByStatusOrderByCreatedAtDesc(AlertStatus.RESOLVIDO)
                : repository.findByColdRoomIdAndStatusOrderByCreatedAtDesc(
                        coldRoomId, AlertStatus.RESOLVIDO);

        return alerts.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void recognize(Integer alertId, Integer userId) {
        entityManager.createNativeQuery(
                        "SELECT set_config('app.usuario_atual', :userId, true)")
                .setParameter("userId", userId.toString())
                .getSingleResult();

        entityManager.createNativeQuery("CALL sp_reconhecer_alerta(:alertId, :userId)")
                .setParameter("alertId", alertId)
                .setParameter("userId", userId)
                .executeUpdate();
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
