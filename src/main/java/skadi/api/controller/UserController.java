package skadi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;
import skadi.api.dto.*;
import skadi.api.model.User;
import skadi.api.service.PasswordRecoveryService;
import skadi.api.service.SupportRequestService;
import skadi.api.service.UserService;

import java.net.URI;

@RestController
@Tag(name = "Usuários")
public class UserController {
    private final UserService service;
    private final SupportRequestService supportRequestService;
    private final PasswordRecoveryService passwordRecoveryService;

    public UserController(
            UserService service,
            SupportRequestService supportRequestService,
            PasswordRecoveryService passwordRecoveryService
    ) {
        this.service = service;
        this.supportRequestService = supportRequestService;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @Operation(summary = "Cadastrar usuário", tags = {"Acesso e autenticação"})
    @PostMapping("/auth/register")
    public ResponseEntity<Void> register(
            @RequestBody RegisterRequest dto,
            @AuthenticationPrincipal User authenticatedUser
    ){
        service.register(dto, authenticatedUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }


    @Operation(summary = "Fazer login", description = "Autentica o usuário e retorna o token JWT.", tags = {"Acesso e autenticação"}, security = {})
    @PostMapping("/auth/login")
    public ResponseEntity<TokenResponse> login (@RequestBody LoginRequest dto){
        return ResponseEntity.ok(service.login(dto));
    }

    @PostMapping("/auth/recover-password")
    public ResponseEntity<Void> recoverPassword(@Valid @RequestBody PasswordRecoveryRequest request) {
        passwordRecoveryService.recoverPassword(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordRecoveryService.resetPassword(request.email(), request.codigo(), request.senha());
        return ResponseEntity.noContent().build();
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
