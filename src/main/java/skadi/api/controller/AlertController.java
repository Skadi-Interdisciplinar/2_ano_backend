package skadi.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.AlertResponse;
import skadi.api.model.User;
import skadi.api.service.AlertService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/alerts")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Alertas")
public class AlertController {

    private final AlertService service;

    @GetMapping
    public ResponseEntity<List<AlertResponse>> findAll(
            @RequestParam(required = false) Integer coldRoomId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.findAll(coldRoomId, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> findById(
            @PathVariable Integer id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.findById(id, user));
    }

    @GetMapping("/history")
    public ResponseEntity<List<AlertResponse>> findHistory(
            @RequestParam(required = false) Integer coldRoomId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.findHistory(coldRoomId, user));
    }

    @PatchMapping("/{id}/recognize")
    public ResponseEntity<Void> recognize(
            @PathVariable Integer id,
            @AuthenticationPrincipal User user
    ) {
        service.recognize(id, user);
        return ResponseEntity.noContent().build();
    }
}
