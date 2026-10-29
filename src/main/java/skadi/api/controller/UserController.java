package skadi.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;
import skadi.api.dto.*;
import skadi.api.model.User;
import skadi.api.service.SupportRequestService;
import skadi.api.service.UserService;

import java.net.URI;

@RestController
public class UserController {
    private final UserService service;
    private final SupportRequestService supportRequestService;

    public UserController(UserService service, SupportRequestService supportRequestService) {
        this.service = service;
        this.supportRequestService = supportRequestService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Void> register(
            @RequestBody RegisterRequest dto,
            @AuthenticationPrincipal User authenticatedUser
    ){
        service.register(dto, authenticatedUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }


    @PostMapping("/auth/login")
    public ResponseEntity<TokenResponse> login (@RequestBody LoginRequest dto){
        return ResponseEntity.ok(service.login(dto));
    }


    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(user);
    }

    @PostMapping("/support")
    public ResponseEntity<SupportRequestResponse> createSupportRequest(
            @Valid @RequestBody SupportRequestRequest request,
            @AuthenticationPrincipal User user
    ) {
        SupportRequestResponse response = supportRequestService.create(request, user);
        return ResponseEntity.created(URI.create("/support/" + response.id())).body(response);
    }
}
