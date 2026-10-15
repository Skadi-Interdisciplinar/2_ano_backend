package skadi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.TemperatureReadingRequest;
import skadi.api.dto.TemperatureReadingResponse;
import skadi.api.dto.TemperatureReadingView;
import skadi.api.exceptions.CurrentTemperatureNotFoundException;
import skadi.api.model.User;
import skadi.api.service.TemperatureReadingService;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class TemperatureReadingController {

    private final TemperatureReadingService service;

    @PostMapping("/reading")
    public ResponseEntity<TemperatureReadingResponse> publish(
            @Valid @RequestBody TemperatureReadingRequest request
    ) {
        TemperatureReadingResponse response = service.publish(request);
        return ResponseEntity.accepted()
                .location(URI.create("/reading/" + response.eventId()))
                .body(response);
    }

    @GetMapping("/readings")
    public ResponseEntity<List<TemperatureReadingView>> findAll(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findAll(user.getCdId(), user.getNivelAcesso()));
    }

    @GetMapping("/readings/{readingId}")
    public ResponseEntity<TemperatureReadingView> findById(
            @PathVariable Integer readingId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findById(readingId, user.getCdId(), user.getNivelAcesso()));
    }

    @GetMapping("/readings/cold-rooms/{coldRoomId}")
    public ResponseEntity<List<TemperatureReadingView>> findByColdRoom(
            @PathVariable Long coldRoomId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(service.findHistoryByColdRoom(
                coldRoomId, user.getCdId(), user.getNivelAcesso(), null, null));
    }

    @GetMapping("/readings/cold-rooms/{coldRoomId}/current-temperature")
    public ResponseEntity<TemperatureReadingView> findCurrentByColdRoom(
            @PathVariable Long coldRoomId,
            @AuthenticationPrincipal User user
    ) {
        return service.findCurrentByColdRoom(coldRoomId, user.getCdId(), user.getNivelAcesso())
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CurrentTemperatureNotFoundException(coldRoomId));
    }

    @GetMapping("/readings/cold-rooms/{coldRoomId}/temperature-history")
    public ResponseEntity<List<TemperatureReadingView>> findHistoryByColdRoom(
            @PathVariable Long coldRoomId,
            @AuthenticationPrincipal User user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to
    ) {
        return ResponseEntity.ok(service.findHistoryByColdRoom(
                coldRoomId,
                user.getCdId(),
                user.getNivelAcesso(),
                from == null ? null : from.toLocalDateTime(),
                to == null ? null : to.toLocalDateTime()
        ));
    }
}
