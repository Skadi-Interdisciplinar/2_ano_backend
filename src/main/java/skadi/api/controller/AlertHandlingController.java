package skadi.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.AlertHandlingResponse;
import skadi.api.service.AlertHandlingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AlertHandlingController {
    private final AlertHandlingService service;

    @GetMapping("/alerts/{alertId}/alert-handling")
    public ResponseEntity<AlertHandlingResponse> findByAlertId(@PathVariable Integer alertId) {
        return ResponseEntity.ok(service.findByAlertId(alertId));
    }

    @GetMapping("/alert-handlings")
    public ResponseEntity<List<AlertHandlingResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/alert-handlings/{id}")
    public ResponseEntity<AlertHandlingResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
