package skadi.api.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skadi.api.dto.JustificationRequest;
import skadi.api.dto.JustificationResponse;
import skadi.api.model.Justification;
import skadi.api.repository.JustificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JustificationService {
    private final JustificationRepository repository;

    public List<JustificationResponse> findByAlertHandlingId(Integer alertHandlingId) {
        return repository.findByAlertHandlingIdOrderByCreatedAtDesc(alertHandlingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public JustificationResponse create(Integer alertHandlingId, JustificationRequest request) {
        Justification justification = new Justification(
                null, alertHandlingId, request.reason(), request.description(), LocalDateTime.now());
        return toResponse(repository.save(justification));
    }

    public JustificationResponse findById(Integer alertHandlingId, Integer id) {
        return repository.findByIdAndAlertHandlingId(id, alertHandlingId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Justification not found for alert handling: " + alertHandlingId));
    }

    @Transactional
    public JustificationResponse update(
            Integer alertHandlingId,
            Integer id,
            JustificationRequest request
    ) {
        Justification justification = repository.findByIdAndAlertHandlingId(id, alertHandlingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Justification not found for alert handling: " + alertHandlingId));
        justification.setReason(request.reason());
        justification.setDescription(request.description());
        return toResponse(repository.save(justification));
    }

    @Transactional
    public void delete(Integer alertHandlingId, Integer id) {
        Justification justification = repository.findByIdAndAlertHandlingId(id, alertHandlingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Justification not found for alert handling: " + alertHandlingId));
        repository.delete(justification);
    }

    private JustificationResponse toResponse(Justification justification) {
        return new JustificationResponse(
                justification.getId(), justification.getAlertHandlingId(), justification.getReason(),
                justification.getDescription(), justification.getCreatedAt());
    }
}
