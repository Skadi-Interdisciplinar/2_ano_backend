package skadi.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.StateResponse;
import skadi.api.service.StateService;

import java.util.List;

@RestController
@RequestMapping("/states")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Estados")
public class StateController {

    private final StateService service;

    @GetMapping
    public ResponseEntity<List<StateResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StateResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
}
