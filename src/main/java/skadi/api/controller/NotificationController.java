package skadi.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.NotificationResponse;
import skadi.api.model.User;
import skadi.api.service.NotificationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Notificações")
public class NotificationController {
    private final NotificationService service;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> findAll(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findAllByUserId(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> findById(
            @PathVariable Integer id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findById(id, user.getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Integer id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.markAsRead(id, user.getId()));
    }
}
