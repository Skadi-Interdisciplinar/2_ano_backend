package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.config.TokenProvider;
import skadi.api.dto.ErrorResponse;
import skadi.api.dto.LoginRequest;
import skadi.api.dto.RegisterRequest;
import skadi.api.dto.TokenResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.UserAlreadyExistsException;
import skadi.api.model.User;
import skadi.api.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(repository, authenticationManager, tokenProvider, passwordEncoder);
        ReflectionTestUtils.setField(userService, "expirationTime", 3600L);
    }

    @Test
    void register_savesUserWhenUsernameIsAvailable() {
        RegisterRequest request = new RegisterRequest(
                "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN, 1, null
        );
        User salvo = new User(10L, "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN, 1, null);
        when(repository.existsByUsername("enzo")).thenReturn(false);
        when(passwordEncoder.encode("senha")).thenReturn("$2a$10$hashed");
        when(repository.save(any(User.class))).thenReturn(salvo);

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertEquals("enzo", captor.getValue().getUsername());
        assertEquals(NivelAcesso.ADMIN, captor.getValue().getNivelAcesso());
        assertEquals("$2a$10$hashed", captor.getValue().getSenha());
    }

    @Test
    void register_throwsWhenCreatingSuperAdmin() {
        RegisterRequest request = new RegisterRequest(
                "Root", "root", "12345678901", "root@email.com", "senha", NivelAcesso.SUPER_ADMIN, 1, null
        );

        AccessDeniedException erro = assertThrows(
                AccessDeniedException.class,
                () -> userService.register(request)
        );
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(erro);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().statusCode());
        assertEquals("SUPER_ADMIN cadastra apenas ADMIN", response.getBody().message());
    }

    @Test
    void register_throwsWhenUsernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN, 1, null
        );
        when(repository.existsByUsername("enzo")).thenReturn(true);

        UserAlreadyExistsException erro = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.register(request)
        );
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUserAlreadyExists(erro);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().statusCode());
        assertEquals("Nome de usuário já utilizado", response.getBody().message());
    }

    @Test
    void login_returnsTokenWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("enzo", "senha");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-gerado");

        TokenResponse response = userService.login(request);

        assertEquals("jwt-gerado", response.token());
        assertEquals(3600L, response.expirationTime());
        assertNotNull(response.validUntil());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_throwsBadCredentialsWhenPasswordIsInvalid() {
        LoginRequest request = new LoginRequest("enzo", "errada");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        BadCredentialsException erro = assertThrows(
                BadCredentialsException.class,
                () -> userService.login(request)
        );
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(erro);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().statusCode());
        assertEquals("Credenciais inválidas", response.getBody().message());
    }

    @Test
    void login_throwsRuntimeWhenUnexpectedErrorOccurs() {
        LoginRequest request = new LoginRequest("enzo", "senha");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new IllegalStateException("falha"));

        RuntimeException erro = assertThrows(RuntimeException.class, () -> userService.login(request));
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRuntime(erro);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().statusCode());
        assertEquals("Erro inesperado: java.lang.IllegalStateException: falha", response.getBody().message());
    }
}
