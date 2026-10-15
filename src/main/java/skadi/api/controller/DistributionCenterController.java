package skadi.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.DistributionCenterRequest;
import skadi.api.dto.DistributionCenterRequestName;
import skadi.api.dto.DistributionCenterResponse;
import skadi.api.service.DistributionCenterService;

import java.util.List;

@RestController
@RequestMapping("/cd")
public class DistributionCenterController {
    private final DistributionCenterService service;

    public DistributionCenterController(DistributionCenterService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DistributionCenterResponse> insertCd(@RequestBody @Valid DistributionCenterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.insertCd(request));
    }

    @GetMapping
    public ResponseEntity<List<DistributionCenterResponse>> findAllCD() {
        return ResponseEntity.ok(service.findAllCD());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistributionCenterResponse> findCDById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findCDById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DistributionCenterResponse> updateCD(
            @PathVariable Long id,
            @RequestBody @Valid DistributionCenterRequest request
    ) {
        return ResponseEntity.ok(service.updateCD(request, id));
    }

    @PatchMapping("/{id}/name")
    public ResponseEntity<DistributionCenterResponse> updateNameCd(
            @PathVariable Long id,
            @RequestBody @Valid DistributionCenterRequestName request
    ) {
        return ResponseEntity.ok(service.updateNameCd(request, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCd(@PathVariable Long id) {
        service.deleteCD(id);
        return ResponseEntity.noContent().build();
    }
}
