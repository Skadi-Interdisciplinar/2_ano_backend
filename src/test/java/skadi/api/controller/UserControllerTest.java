package skadi.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.RegisterResponse;
import skadi.api.dto.TokenResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.UserAlreadyExistsException;
import skadi.api.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private static final String ADMIN_BEARER = "Bearer eyJhbGciOiJIUzI1NiJ9.admin-token";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_returns201() throws Exception {
        when(userService.register(any())).thenReturn(
                new RegisterResponse(1L, "Enzo", "enzo", "enzo@email.com", NivelAcesso.OPERADOR, 1)
        );

        mockMvc.perform(post("/register")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Enzo",
                                  "username": "enzo",
                                  "cpf": "12345678901",
                                  "email": "enzo@email.com",
                                  "senha": "senha",
                                  "nivelAcesso": "OPERADOR",
                                  "codCD": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("enzo"));
    }

    @Test
    void login_returns200WithToken() throws Exception {
        when(userService.login(any())).thenReturn(new TokenResponse("jwt-gerado", 3600L));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "enzo", "senha": "senha" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-gerado"))
                .andExpect(jsonPath("$.expirationTime").value(3600));
    }

    @Test
    void register_returns409WhenUsernameExists() throws Exception {
        when(userService.register(any())).thenThrow(new UserAlreadyExistsException());

        mockMvc.perform(post("/register")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Enzo",
                                  "username": "enzo",
                                  "cpf": "12345678901",
                                  "email": "enzo@email.com",
                                  "senha": "senha",
                                  "nivelAcesso": "OPERADOR",
                                  "codCD": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("Nome de usuário já utilizado"));
    }

    @Test
    void register_returns403WhenCreatingSuperAdmin() throws Exception {
        when(userService.register(any()))
                .thenThrow(new AccessDeniedException("ADMIN não pode cadastrar SUPER_ADMIN"));

        mockMvc.perform(post("/register")
                        .header(HttpHeaders.AUTHORIZATION, ADMIN_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Root",
                                  "username": "root",
                                  "cpf": "12345678901",
                                  "email": "root@email.com",
                                  "senha": "senha",
                                  "nivelAcesso": "SUPER_ADMIN",
                                  "codCD": 1
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("ADMIN não pode cadastrar SUPER_ADMIN"));
    }

    @Test
    void login_returns401WhenCredentialsAreInvalid() throws Exception {
        when(userService.login(any())).thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "enzo", "senha": "errada" }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    void login_returns500WhenUnexpectedErrorOccurs() throws Exception {
        when(userService.login(any())).thenThrow(
                new RuntimeException("Erro inesperado: java.lang.IllegalStateException: falha")
        );

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "enzo", "senha": "senha" }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.message").value("Erro inesperado: java.lang.IllegalStateException: falha"));
    }
}
