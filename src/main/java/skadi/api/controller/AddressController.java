package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.AddressRequest;
import skadi.api.dto.AddressResponse;
import skadi.api.service.AddressService;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AddressController {

    private final AddressService service;

    @PostMapping("/distribution-centers/{distributionCenterId}/addresses")
    public ResponseEntity<AddressResponse> create(
            @PathVariable Long distributionCenterId,
            @Valid @RequestBody AddressRequest request
    ) {
        AddressResponse response = service.create(distributionCenterId, request);
        return ResponseEntity.created(URI.create("/addresses/" + response.id())).body(response);
    }

    @GetMapping("/distribution-centers/{distributionCenterId}/addresses")
    public ResponseEntity<List<AddressResponse>> findAllByDistributionCenter(
            @PathVariable Long distributionCenterId
    ) {
        return ResponseEntity.ok(service.findAllByDistributionCenter(distributionCenterId));
    }

    @GetMapping("/addresses/{id}")
    public ResponseEntity<AddressResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<AddressResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
