package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.DistributionCenterRequest;
import skadi.api.exceptions.CdNotFoundException;
import skadi.api.model.DistributionCenter;
import skadi.api.service.DistributionCenterService;

import java.util.List;

@RestController
@RequestMapping("/cd")
public class DistributionCenterController {
    private final DistributionCenterService service;

    public DistributionCenterController( DistributionCenterService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DistributionCenter> insertCd(@RequestBody @Valid DistributionCenterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.insertCd(request));
    }

    @GetMapping
    public ResponseEntity<List<DistributionCenter>> findAllCD() {
        return ResponseEntity.ok(service.findAllCD());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistributionCenter> findCDById(@PathVariable  Long id) {
        return service.findCDById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(CdNotFoundException::new);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DistributionCenter> updateCD(
            @PathVariable Long id,
            @RequestBody @Valid DistributionCenterRequest request
    ) {
        return ResponseEntity.ok(service.updateCD(request, id));
    }

    @PatchMapping("/{id}/name")
    public ResponseEntity<DistributionCenter> updateNameCd(
            @PathVariable Long id,
            @RequestParam String name
    ) {
        return ResponseEntity.ok(service.updateNameCd(name, id));
    }
}
