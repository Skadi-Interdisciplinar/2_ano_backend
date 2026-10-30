package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.SignatureRequest;
import skadi.api.dto.SignatureResponse;
import skadi.api.model.User;
import skadi.api.service.SignatureService;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService service;

    @PostMapping("/reports/{reportId}/signature")
    public ResponseEntity<SignatureResponse> create(
            @PathVariable Integer reportId,
            @Valid @RequestBody SignatureRequest request,
            @AuthenticationPrincipal User user
    ) {
        SignatureResponse signature = service.create(reportId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(signature);
    }

    @GetMapping("/reports/{reportId}/signature")
    public ResponseEntity<SignatureResponse> findByReportId(
            @PathVariable Integer reportId,
            @AuthenticationPrincipal User user
    ) {
        Optional<SignatureResponse> signature = service.findByReportId(
                reportId, user);
        return signature.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/signatures")
    public ResponseEntity<List<SignatureResponse>> findAll(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findAllByCd(user));
    }

    @GetMapping("/signatures/{id}")
    public ResponseEntity<SignatureResponse> findById(
            @PathVariable Integer id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findById(id, user));
    }

}
