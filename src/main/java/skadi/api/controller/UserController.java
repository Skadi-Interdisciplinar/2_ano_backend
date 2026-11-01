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
        if (request.senha() == null || request.senha().isBlank()) {
            passwordRecoveryService.recoverPassword(request.email());
        } else {
            passwordRecoveryService.resetPassword(request.email(), request.senha());
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/verify-recovery-code")
    public ResponseEntity<Void> verifyRecoveryCode(@Valid @RequestBody PasswordCodeVerificationRequest request) {
        passwordRecoveryService.verifyCode(request.email(), request.codigo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal User user
    ) {
        service.changePassword(user, request.senhaAtual(), request.novaSenha(), request.confirmacaoSenha());
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
