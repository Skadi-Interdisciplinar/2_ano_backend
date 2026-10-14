package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.ReportRequest;
import skadi.api.dto.ReportPatchRequest;
import skadi.api.dto.ReportResponse;
import skadi.api.dto.ReportStatusUpdateRequest;
import skadi.api.model.User;
import skadi.api.service.ReportService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final ReportService service;

    @PostMapping
    public ResponseEntity<ReportResponse> create(
            @Valid @RequestBody ReportRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request, Math.toIntExact(user.getId())));
    }

    @GetMapping
    public ResponseEntity<List<ReportResponse>> findAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.findAll(user.getCodCD(), user.getNivelAcesso()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> findById(
            @PathVariable Integer id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findById(id, user.getCodCD(), user.getNivelAcesso()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReportResponse> update(
            @PathVariable Integer id,
            @RequestBody ReportPatchRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.update(id, request, Math.toIntExact(user.getId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id,
            @AuthenticationPrincipal User user
    ) {
        service.delete(id, Math.toIntExact(user.getId()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReportResponse> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody ReportStatusUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.updateStatus(id, request.status(), Math.toIntExact(user.getId())));
    }
}
