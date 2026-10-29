package skadi.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.AlertHandlingResponse;
import skadi.api.model.User;
import skadi.api.service.AlertHandlingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Atendimentos de alertas")
public class AlertHandlingController {
    private final AlertHandlingService service;

    @GetMapping("/alerts/{alertId}/alert-handling")
    public ResponseEntity<AlertHandlingResponse> findByAlertId(
            @PathVariable Integer alertId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findByAlertId(alertId, user));
    }

    @GetMapping("/alert-handlings")
    public ResponseEntity<List<AlertHandlingResponse>> findAll(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findAll(user));
    }

    @GetMapping("/alert-handlings/{id}")
    public ResponseEntity<AlertHandlingResponse> findById(
            @PathVariable Integer id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findById(id, user));
    }
}
