package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.JustificationRequest;
import skadi.api.dto.JustificationResponse;
import skadi.api.service.JustificationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/alert-handlings/{alertHandlingId}/justifications")
public class JustificationController {
    private final JustificationService service;

    @GetMapping
    public ResponseEntity<List<JustificationResponse>> findByAlertHandlingId(
            @PathVariable Integer alertHandlingId) {
        return ResponseEntity.ok(service.findByAlertHandlingId(alertHandlingId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JustificationResponse> findById(
            @PathVariable Integer alertHandlingId,
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(service.findById(alertHandlingId, id));
    }

    @PostMapping
    public ResponseEntity<JustificationResponse> create(
            @PathVariable Integer alertHandlingId,
            @Valid @RequestBody JustificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(alertHandlingId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JustificationResponse> update(
            @PathVariable Integer alertHandlingId,
            @PathVariable Integer id,
            @Valid @RequestBody JustificationRequest request) {
        return ResponseEntity.ok(service.update(alertHandlingId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer alertHandlingId,
            @PathVariable Integer id
    ) {
        service.delete(alertHandlingId, id);
        return ResponseEntity.noContent().build();
    }
}
