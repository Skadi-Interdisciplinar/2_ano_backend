package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.BatchStorageRequest;
import skadi.api.dto.BatchStorageResponse;
import skadi.api.service.BatchStorageService;
import skadi.api.model.User;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/batches/{batchId}/storages")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Armazenamento de lotes")
public class BatchStorageController {

    private final BatchStorageService service;

    @PostMapping
    public ResponseEntity<BatchStorageResponse> enter(
            @PathVariable Long batchId,
            @Valid @RequestBody BatchStorageRequest request,
            @AuthenticationPrincipal User user
    ) {
        BatchStorageResponse response = service.enter(batchId, request, user);
        return ResponseEntity.created(
                URI.create("/batches/" + batchId + "/storages/" + response.id())
        ).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BatchStorageResponse>> findHistory(
            @PathVariable Long batchId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findHistory(batchId, user));
    }

    @PutMapping("/{storageId}")
    public ResponseEntity<BatchStorageResponse> move(
            @PathVariable Long batchId,
            @PathVariable Long storageId,
            @Valid @RequestBody BatchStorageRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.move(batchId, storageId, request, user));
    }

    @PatchMapping("/{storageId}/exit")
    public ResponseEntity<BatchStorageResponse> exit(
            @PathVariable Long batchId,
            @PathVariable Long storageId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.exit(batchId, storageId, user));
    }
}
