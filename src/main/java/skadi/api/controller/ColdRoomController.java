package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.ColdRoomRequest;
import skadi.api.dto.ColdRoomResponse;
import skadi.api.model.User;
import skadi.api.service.ColdRoomService;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ColdRoomController {

    private final ColdRoomService service;

    @PostMapping("/cold-room")
    public ResponseEntity<ColdRoomResponse> create(
            @Valid @RequestBody ColdRoomRequest request,
            @AuthenticationPrincipal User user
    ) {
        ColdRoomResponse response = service.create(request, user);
        return ResponseEntity.created(URI.create("/cold-rooms/" + response.id()))
                .body(response);
    }

    @GetMapping("/cold-rooms")
    public ResponseEntity<List<ColdRoomResponse>> findAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.findAll(user));
    }

    @GetMapping("/cold-rooms/{id}")
    public ResponseEntity<ColdRoomResponse> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findById(id, user));
    }

    @PutMapping("/cold-rooms/{id}")
    public ResponseEntity<ColdRoomResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ColdRoomRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.update(id, request, user));
    }

    @DeleteMapping("/cold-rooms/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        service.delete(id, user);
        return ResponseEntity.noContent().build();
    }
}
