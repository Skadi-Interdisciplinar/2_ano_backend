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
import skadi.api.dto.ThermometerRequest;
import skadi.api.dto.ThermometerResponse;
import skadi.api.service.ThermometerService;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ThermometerController {

    private final ThermometerService service;

    @PostMapping("/thermometer")
    public ResponseEntity<ThermometerResponse> create(
            @Valid @RequestBody ThermometerRequest request
    ) {
        ThermometerResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/thermometers/" + response.id()))
                .body(response);
    }

    @GetMapping("/thermometers")
    public ResponseEntity<List<ThermometerResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/thermometers/{id}")
    public ResponseEntity<ThermometerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/thermometers/{id}")
    public ResponseEntity<ThermometerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ThermometerRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/thermometers/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
