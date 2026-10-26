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
import skadi.api.dto.AdminListResponse;
import skadi.api.dto.TokenResponse;
import skadi.api.dto.UserResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.UserAlreadyExistsException;
import skadi.api.exceptions.UserNotFoundException;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;
import skadi.api.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
        DistributionCenter cd = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        RegisterRequest request = new RegisterRequest(
                "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN,
                null, List.of(cd), null
        );
        User salvo = new User(10L, "Enzo", "enzo", "12345678901", "enzo@email.com", "senha",
                NivelAcesso.ADMIN, null, List.of(cd), null);
        when(repository.existsByUsername("enzo")).thenReturn(false);
        when(passwordEncoder.encode("senha")).thenReturn("$2a$10$hashed");
        when(repository.save(any(User.class))).thenReturn(salvo);

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertEquals("enzo", captor.getValue().getUsername());
        assertEquals(NivelAcesso.ADMIN, captor.getValue().getNivelAcesso());
        assertEquals("$2a$10$hashed", captor.getValue().getSenha());
        assertEquals(List.of(cd), captor.getValue().getCd());
        assertNull(captor.getValue().getCdGestor());
    }

    @Test
    void register_throwsWhenCreatingSuperAdmin() {
        RegisterRequest request = new RegisterRequest(
                "Root", "root", "12345678901", "root@email.com", "senha", NivelAcesso.SUPER_ADMIN,
                null, List.of(new DistributionCenter(1L, "CD Osasco", "12345678901234", null)), null
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
                "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN,
                null, List.of(new DistributionCenter(1L, "CD Osasco", "12345678901234", null)), null
        );
        when(repository.existsByUsername("enzo")).thenReturn(true);

        UserAlreadyExistsException erro = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.register(request)
        );
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAlreadyExists(erro);

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

    @Test
    void getAdminsSvc_returnsMappedAdminUsers() {
        DistributionCenter cd = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        User admin = new User(10L, "Enzo", "enzo", "12345678901", "enzo@email.com", "senha",
                NivelAcesso.ADMIN, null, List.of(cd), 10);
        when(repository.findAllByNivelAcesso(NivelAcesso.ADMIN)).thenReturn(List.of(admin));

        AdminListResponse response = userService.getAdminsSvc();

        assertEquals(1, response.total());
        assertEquals(1, response.usuarios().size());
        assertEquals(10L, response.usuarios().getFirst().id());
        assertEquals("Enzo", response.usuarios().getFirst().nome());
        assertEquals("enzo", response.usuarios().getFirst().username());
        assertEquals("enzo@email.com", response.usuarios().getFirst().email());
        assertEquals(NivelAcesso.ADMIN, response.usuarios().getFirst().nivelAcesso());
        assertEquals(List.of(cd), response.usuarios().getFirst().cd());
        verify(repository).findAllByNivelAcesso(NivelAcesso.ADMIN);
    }

    @Test
    void getAdminsSvc_returnsEmptyListWhenThereAreNoAdmins() {
        when(repository.findAllByNivelAcesso(NivelAcesso.ADMIN)).thenReturn(List.of());

        AdminListResponse response = userService.getAdminsSvc();

        assertEquals(0, response.total());
        assertEquals(List.of(), response.usuarios());
        verify(repository).findAllByNivelAcesso(NivelAcesso.ADMIN);
    }

    @Test
    void createAdmin_savesAndReturnsAdmin() {
        DistributionCenter cd = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        RegisterRequest request = new RegisterRequest(
                "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN,
                null, List.of(cd), null
        );
        User salvo = new User(10L, "Enzo", "enzo", "12345678901", "enzo@email.com", "$2a$10$hashed",
                NivelAcesso.ADMIN, null, List.of(cd), null);
        when(repository.existsByUsername("enzo")).thenReturn(false);
        when(repository.existsByEmail("enzo@email.com")).thenReturn(false);
        when(repository.existsByCpf("12345678901")).thenReturn(false);
        when(passwordEncoder.encode("senha")).thenReturn("$2a$10$hashed");
        when(repository.save(any(User.class))).thenReturn(salvo);

        UserResponse response = userService.createAdmin(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        assertEquals("$2a$10$hashed", captor.getValue().getSenha());
        assertNull(captor.getValue().getCdGestor());
        assertEquals(10L, response.id());
        assertEquals("enzo", response.username());
        assertEquals(NivelAcesso.ADMIN, response.nivelAcesso());
        assertEquals(List.of(cd), response.cd());
    }

    @Test
    void createAdmin_throwsWhenRoleIsNotAdmin() {
        RegisterRequest request = new RegisterRequest(
                "Root", "root", "12345678901", "root@email.com", "senha", NivelAcesso.SUPER_ADMIN,
                null, List.of(), null
        );

        AccessDeniedException erro = assertThrows(AccessDeniedException.class, () -> userService.createAdmin(request));
        assertEquals("SUPER_ADMIN cadastra apenas ADMIN", erro.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void updateAdmin_updatesExistingAdmin() {
        DistributionCenter cd = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        DistributionCenter novoCd = new DistributionCenter(2L, "CD Barueri", "12345678901235", null);
        User admin = new User(10L, "Enzo", "enzo", "12345678901", "enzo@email.com", "senhaAntiga",
                NivelAcesso.ADMIN, null, List.of(cd), null);
        RegisterRequest request = new RegisterRequest(
                "Enzo Atualizado", "enzo.novo", "10987654321", "novo@email.com", "novaSenha",
                NivelAcesso.ADMIN, null, List.of(novoCd), 4
        );
        when(repository.findByIdAndNivelAcesso(10L, NivelAcesso.ADMIN)).thenReturn(Optional.of(admin));
        when(repository.existsByUsername("enzo.novo")).thenReturn(false);
        when(repository.existsByEmail("novo@email.com")).thenReturn(false);
        when(repository.existsByCpf("10987654321")).thenReturn(false);
        when(passwordEncoder.encode("novaSenha")).thenReturn("$2a$10$nova");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateAdmin(10L, request);

        assertEquals("Enzo Atualizado", response.nome());
        assertEquals("enzo.novo", response.username());
        assertEquals("novo@email.com", response.email());
        assertEquals(List.of(novoCd), response.cd());
        assertEquals("$2a$10$nova", admin.getSenha());
        assertNull(admin.getCdGestor());
    }

    @Test
    void updateAdmin_throwsWhenAdminDoesNotExist() {
        RegisterRequest request = new RegisterRequest(
                "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN,
                null, List.of(), null
        );
        when(repository.findByIdAndNivelAcesso(99L, NivelAcesso.ADMIN)).thenReturn(Optional.empty());

        UserNotFoundException erro = assertThrows(UserNotFoundException.class,
                () -> userService.updateAdmin(99L, request));
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourcesNotFound(erro);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Administrador não encontrado: 99", response.getBody().message());
    }

    @Test
    void deleteAdmin_removesExistingAdmin() {
        DistributionCenter cd = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        User admin = new User(10L, "Enzo", "enzo", "12345678901", "enzo@email.com", "senha",
                NivelAcesso.ADMIN, null, List.of(cd), null);
        when(repository.findByIdAndNivelAcesso(10L, NivelAcesso.ADMIN)).thenReturn(Optional.of(admin));

        userService.deleteAdmin(10L);

        verify(repository).delete(admin);
    }

    @Test
    void deleteAdmin_throwsWhenAdminDoesNotExist() {
        when(repository.findByIdAndNivelAcesso(99L, NivelAcesso.ADMIN)).thenReturn(Optional.empty());

        UserNotFoundException erro = assertThrows(UserNotFoundException.class, () -> userService.deleteAdmin(99L));
        assertEquals("Administrador não encontrado: 99", erro.getMessage());
        verify(repository, never()).delete(any());
    }
}
