package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skadi.api.dto.AlertHandlingResponse;
import skadi.api.exceptions.AlertHandlingNotFoundException;
import skadi.api.model.AlertHandling;
import skadi.api.model.User;
import skadi.api.repository.AlertRepository;
import skadi.api.repository.AlertHandlingRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertHandlingService {
    private final AlertHandlingRepository repository;
    private final AlertRepository alertRepository;
    private final DistributionCenterAccessService accessService;

    public List<AlertHandlingResponse> findAll(User user) {
        return repository.findAllByOrderByIdDesc().stream()
                .filter(handling -> accessService.canRead(
                        user, distributionCenterId(handling.getAlertId())))
                .map(this::toResponse)
                .toList();
    }

    public AlertHandlingResponse findByAlertId(Integer alertId, User user) {
        AlertHandling handling = repository.findFirstByAlertIdOrderByIdDesc(alertId)
                .orElseThrow(() -> AlertHandlingNotFoundException.forAlertId(alertId));
        accessService.requireRead(user, distributionCenterId(alertId));
        return toResponse(handling);
    }

    public AlertHandlingResponse findById(Integer id, User user) {
        AlertHandling handling = repository.findById(id)
                .orElseThrow(() -> AlertHandlingNotFoundException.forHandlingId(id));
        accessService.requireRead(user, distributionCenterId(handling.getAlertId()));
        return toResponse(handling);
    }

    private Long distributionCenterId(Integer alertId) {
        return alertRepository.findDistributionCenterIdByAlertId(alertId)
                .orElseThrow(() -> AlertHandlingNotFoundException.forAlertId(alertId));
    }

    private AlertHandlingResponse toResponse(AlertHandling handling) {
        return new AlertHandlingResponse(
                handling.getId(), handling.getAlertId(), handling.getUserId(),
                handling.getAcknowledgedAt(), handling.getResolvedAt(), handling.getStatus());
    }
}
