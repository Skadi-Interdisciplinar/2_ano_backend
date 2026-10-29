package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.JustificationRequest;
import skadi.api.dto.JustificationPatchRequest;
import skadi.api.dto.JustificationResponse;
import skadi.api.model.User;
import skadi.api.service.JustificationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/alert-handlings/{alertHandlingId}/justification")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Justificativas")
public class JustificationController {
    private final JustificationService service;

    @GetMapping
    public ResponseEntity<JustificationResponse> findByAlertHandlingId(
            @PathVariable Integer alertHandlingId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.findByAlertHandlingId(alertHandlingId, user));
    }

    @PostMapping
    public ResponseEntity<JustificationResponse> create(
            @PathVariable Integer alertHandlingId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody JustificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(alertHandlingId, request, user));
    }

    @PatchMapping
    public ResponseEntity<JustificationResponse> update(
            @PathVariable Integer alertHandlingId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody JustificationPatchRequest request) {
        return ResponseEntity.ok(service.update(alertHandlingId, request, user));
    }

}
