package skadi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;
import skadi.api.dto.*;
import skadi.api.model.User;
import skadi.api.service.UserService;

@RestController
@Tag(name = "Usuários")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
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


    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(user);
    }
}
