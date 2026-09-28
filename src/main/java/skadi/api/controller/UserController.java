package skadi.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;
import skadi.api.dto.*;
import skadi.api.model.User;
import skadi.api.service.UserService;

@RestController
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest dto){
        service.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }


    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login (@RequestBody LoginRequest dto){
        return ResponseEntity.ok(service.login(dto));
    }


    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(user);
    }
}
