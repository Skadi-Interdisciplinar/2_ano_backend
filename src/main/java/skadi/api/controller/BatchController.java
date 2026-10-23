package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.BatchRequest;
import skadi.api.dto.BatchResponse;
import skadi.api.dto.BatchStatusUpdateRequest;
import skadi.api.service.BatchService;
import skadi.api.model.User;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService service;

    @PostMapping
    public ResponseEntity<BatchResponse> create(
            @Valid @RequestBody BatchRequest request,
            @AuthenticationPrincipal User user
    ) {
        BatchResponse response = service.create(request, user);
        return ResponseEntity.created(URI.create("/batches/" + response.id()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<BatchResponse>> findAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.findAll(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BatchResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findById(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BatchResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BatchRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.update(id, request, user));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BatchResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody BatchStatusUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.updateStatus(id, request.status(), user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        service.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
