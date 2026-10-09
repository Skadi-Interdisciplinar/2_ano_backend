package skadi.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.AlertResponse;
import skadi.api.dto.AlertUpdateRequest;
import skadi.api.model.User;
import skadi.api.service.AlertService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService service;

    @GetMapping
    public ResponseEntity<List<AlertResponse>> findAll(
            @RequestParam(required = false) Integer coldRoomId) {
        return ResponseEntity.ok(service.findAll(coldRoomId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody AlertUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<AlertResponse>> findHistory(
            @RequestParam(required = false) Integer coldRoomId) {
        return ResponseEntity.ok(service.findHistory(coldRoomId));
    }

    @PatchMapping("/{id}/recognize")
    public ResponseEntity<Void> recognize(
            @PathVariable Integer id,
            @AuthenticationPrincipal User user
    ) {
        service.recognize(id, Math.toIntExact(user.getId()));
        return ResponseEntity.noContent().build();
    }
}
