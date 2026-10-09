package skadi.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.AlertHandlingResponse;
import skadi.api.dto.AlertHandlingUpdateRequest;
import skadi.api.service.AlertHandlingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/alerts/{alertId}/alert-handlings")
public class AlertHandlingController {
    private final AlertHandlingService service;

    @GetMapping
    public ResponseEntity<List<AlertHandlingResponse>> findByAlertId(@PathVariable Integer alertId) {
        return ResponseEntity.ok(service.findByAlertId(alertId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertHandlingResponse> findById(
            @PathVariable Integer alertId,
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(service.findById(alertId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertHandlingResponse> update(
            @PathVariable Integer alertId,
            @PathVariable Integer id,
            @Valid @RequestBody AlertHandlingUpdateRequest request) {
        return ResponseEntity.ok(service.update(alertId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer alertId,
            @PathVariable Integer id
    ) {
        service.delete(alertId, id);
        return ResponseEntity.noContent().build();
    }
}
