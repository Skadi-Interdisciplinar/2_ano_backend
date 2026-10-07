package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skadi.api.dto.AlertHandlingResponse;
import skadi.api.dto.AlertHandlingUpdateRequest;
import skadi.api.model.AlertHandling;
import skadi.api.repository.AlertHandlingRepository;

import java.util.List;
import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class AlertHandlingService {
    private final AlertHandlingRepository repository;

    public List<AlertHandlingResponse> findByAlertId(Integer alertId) {
        return repository.findByAlertIdOrderByIdDesc(alertId).stream()
                .map(this::toResponse)
                .toList();
    }

    public AlertHandlingResponse findById(Integer alertId, Integer id) {
        return repository.findByIdAndAlertId(id, alertId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Alert handling not found for alert: " + alertId));
    }

    @Transactional
    public AlertHandlingResponse update(
            Integer alertId,
            Integer id,
            AlertHandlingUpdateRequest request
    ) {
        AlertHandling handling = repository.findByIdAndAlertId(id, alertId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Alert handling not found for alert: " + alertId));
        handling.setStatus(request.status());
        return toResponse(repository.save(handling));
    }

    @Transactional
    public void delete(Integer alertId, Integer id) {
        AlertHandling handling = repository.findByIdAndAlertId(id, alertId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Alert handling not found for alert: " + alertId));
        repository.delete(handling);
    }

    private AlertHandlingResponse toResponse(AlertHandling handling) {
        return new AlertHandlingResponse(
                handling.getId(), handling.getAlertId(), handling.getUserId(),
                handling.getAcknowledgedAt(), handling.getResolvedAt(), handling.getStatus());
    }
}
