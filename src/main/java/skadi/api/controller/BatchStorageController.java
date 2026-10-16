package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/batches/{batchId}/storages")
@RequiredArgsConstructor
public class BatchStorageController {

    private final BatchStorageService service;

    @PostMapping
    public ResponseEntity<BatchStorageResponse> enter(
            @PathVariable Long batchId,
            @Valid @RequestBody BatchStorageRequest request
    ) {
        BatchStorageResponse response = service.enter(batchId, request);
        return ResponseEntity.created(
                URI.create("/batches/" + batchId + "/storages/" + response.id())
        ).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BatchStorageResponse>> findHistory(@PathVariable Long batchId) {
        return ResponseEntity.ok(service.findHistory(batchId));
    }

    @PutMapping("/{storageId}")
    public ResponseEntity<BatchStorageResponse> move(
            @PathVariable Long batchId,
            @PathVariable Long storageId,
            @Valid @RequestBody BatchStorageRequest request
    ) {
        return ResponseEntity.ok(service.move(batchId, storageId, request));
    }

    @PatchMapping("/{storageId}/exit")
    public ResponseEntity<BatchStorageResponse> exit(
            @PathVariable Long batchId,
            @PathVariable Long storageId
    ) {
        return ResponseEntity.ok(service.exit(batchId, storageId));
    }
}
