package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skadi.api.dto.AlertHandlingResponse;
import skadi.api.exceptions.AlertHandlingNotFoundException;
import skadi.api.model.AlertHandling;
import skadi.api.repository.AlertHandlingRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertHandlingService {
    private final AlertHandlingRepository repository;

    public List<AlertHandlingResponse> findAll() {
        return repository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public AlertHandlingResponse findByAlertId(Integer alertId) {
        return repository.findFirstByAlertIdOrderByIdDesc(alertId)
                .map(this::toResponse)
                .orElseThrow(() -> AlertHandlingNotFoundException.forAlertId(alertId));
    }

    public AlertHandlingResponse findById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> AlertHandlingNotFoundException.forHandlingId(id));
    }

    private AlertHandlingResponse toResponse(AlertHandling handling) {
        return new AlertHandlingResponse(
                handling.getId(), handling.getAlertId(), handling.getUserId(),
                handling.getAcknowledgedAt(), handling.getResolvedAt(), handling.getStatus());
    }
}
