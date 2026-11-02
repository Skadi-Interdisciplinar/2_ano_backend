package skadi.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.SupportRequestResponse;
import skadi.api.dto.TokenResponse;
import skadi.api.enums.SupportCategory;
import skadi.api.enums.SupportStatus;
import skadi.api.exceptions.InvalidPasswordRecoveryCodeException;
import skadi.api.exceptions.PasswordConfirmationMismatchException;
import skadi.api.exceptions.SupportRequestRuleViolationException;
import skadi.api.exceptions.UserAlreadyExistsException;
import skadi.api.exceptions.UserNotFoundException;
import skadi.api.model.User;
import skadi.api.service.PasswordRecoveryService;
import skadi.api.service.SupportRequestService;
import skadi.api.service.UserService;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private SupportRequestService supportRequestService;

    @Mock
    private PasswordRecoveryService passwordRecoveryService;

    private static final String SUPER_ADMIN_BEARER = "Bearer eyJhbGciOiJIUzI1NiJ9.admin-token";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService, supportRequestService, passwordRecoveryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUserResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void register_returns201() throws Exception {
        doNothing().when(userService).register(any(), any());

        mockMvc.perform(post("/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Enzo",
                                  "username": "enzo",
                                  "cpf": "12345678901",
                                  "email": "enzo@email.com",
                                  "senha": "senha",
                                  "nivelAcesso": "ADMIN",
                                  "cdGestor": null,
                                  "cd": [{ "id": 1, "name": "CD Osasco", "cnpj": "12345678901234" }],
                                  "codGestor": null
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void login_returns200WithToken() throws Exception {
        Timestamp validUntil = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"));
        when(userService.login(any())).thenReturn(new TokenResponse("jwt-gerado", 1800L, validUntil));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "enzo", "password": "senha" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-gerado"))
                .andExpect(jsonPath("$.expirationTime").value(1800))
                .andExpect(jsonPath("$.validUntil").value("2026-01-01T00:00:00.000Z"));
    }

    @Test
    void register_returns409WhenUsernameExists() throws Exception {
        doThrow(new UserAlreadyExistsException()).when(userService).register(any(), any());

        mockMvc.perform(post("/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Enzo",
                                  "username": "enzo",
                                  "cpf": "12345678901",
                                  "email": "enzo@email.com",
                                  "senha": "senha",
                                  "nivelAcesso": "ADMIN",
                                  "cdGestor": null,
                                  "cd": [{ "id": 1, "name": "CD Osasco", "cnpj": "12345678901234" }],
                                  "codGestor": null
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("Nome de usuário já utilizado"));
    }

    @Test
    void register_returns403WhenCreatingSuperAdmin() throws Exception {
        doThrow(new AccessDeniedException("SUPER_ADMIN cadastra apenas ADMIN"))
                .when(userService).register(any(), any());

        mockMvc.perform(post("/auth/register")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Root",
                                  "username": "root",
                                  "cpf": "12345678901",
                                  "email": "root@email.com",
                                  "senha": "senha",
                                  "nivelAcesso": "SUPER_ADMIN",
                                  "cdGestor": null,
                                  "cd": [{ "id": 1, "name": "CD Osasco", "cnpj": "12345678901234" }],
                                  "codGestor": null
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("SUPER_ADMIN cadastra apenas ADMIN"));
    }

    @Test
    void login_returns401WhenCredentialsAreInvalid() throws Exception {
        when(userService.login(any())).thenThrow(new BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "enzo", "password": "errada" }
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

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "enzo", "password": "senha" }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.message").value("Erro inesperado: java.lang.IllegalStateException: falha"));
    }

    @Test
    void createSupportRequestReturns201() throws Exception {
        when(supportRequestService.create(any(), any())).thenReturn(new SupportRequestResponse(
                1L,
                4L,
                SupportCategory.DUVIDA,
                "Não consigo abrir o relatório.",
                SupportStatus.REGISTRADO,
                LocalDateTime.of(2026, 10, 29, 8, 25),
                null,
                null,
                null
        ));

        mockMvc.perform(post("/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoria": "DUVIDA",
                                  "descricao": "Não consigo abrir o relatório."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/support/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codUsuarioSolicitante").value(4))
                .andExpect(jsonPath("$.categoria").value("DUVIDA"))
                .andExpect(jsonPath("$.status").value("REGISTRADO"))
                .andExpect(jsonPath("$.dataHoraAbertura").value("2026-10-29T08:25:00"));
    }

    @Test
    void createSupportRequestReturns400WhenStatusIsNotRegistrado() throws Exception {
        when(supportRequestService.create(any(), any())).thenThrow(
                new SupportRequestRuleViolationException(
                        "O chamado deve ser aberto com o status REGISTRADO"));

        mockMvc.perform(post("/support")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoria": "DUVIDA",
                                  "descricao": "Não consigo abrir o relatório.",
                                  "status": "EM_ATENDIMENTO"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("O chamado deve ser aberto com o status REGISTRADO"));
    }

    @Test
    void recoverPasswordReturns204() throws Exception {
        doNothing().when(passwordRecoveryService).recoverPassword("enzo@email.com");

        mockMvc.perform(post("/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "enzo@email.com" }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void recoverPasswordReturns404WhenEmailDoesNotExist() throws Exception {
        doThrow(new UserNotFoundException("E-mail não encontrado"))
                .when(passwordRecoveryService).recoverPassword("ausente@email.com");

        mockMvc.perform(post("/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "ausente@email.com" }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("E-mail não encontrado"));
    }

    @Test
    void recoverPasswordChangesPasswordAfterCodeWasVerified() throws Exception {
        doNothing().when(passwordRecoveryService).resetPassword("enzo@email.com", "novaSenha");

        mockMvc.perform(post("/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "enzo@email.com",
                                  "senha": "novaSenha"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void recoverPasswordReturns400WhenCodeWasNotVerified() throws Exception {
        doThrow(new InvalidPasswordRecoveryCodeException())
                .when(passwordRecoveryService).resetPassword("enzo@email.com", "novaSenha");

        mockMvc.perform(post("/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "enzo@email.com",
                                  "senha": "novaSenha"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Código de recuperação inválido ou expirado"));
    }

    @Test
    void recoverPasswordReturns404WhenResetEmailDoesNotExist() throws Exception {
        doThrow(new UserNotFoundException("E-mail não encontrado"))
                .when(passwordRecoveryService).resetPassword("ausente@email.com", "novaSenha");

        mockMvc.perform(post("/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ausente@email.com",
                                  "senha": "novaSenha"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("E-mail não encontrado"));
    }

    @Test
    void recoverPasswordReturns400WhenEmailIsBlank() throws Exception {
        mockMvc.perform(post("/auth/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("email: O e-mail é obrigatório"));

        verifyNoInteractions(passwordRecoveryService);
    }

    @Test
    void verifyRecoveryCodeReturns204() throws Exception {
        doNothing().when(passwordRecoveryService).verifyCode("enzo@email.com", "123456");

        mockMvc.perform(post("/auth/verify-recovery-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "enzo@email.com",
                                  "codigo": "123456"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void verifyRecoveryCodeReturns400WhenCodeIsInvalid() throws Exception {
        doThrow(new InvalidPasswordRecoveryCodeException())
                .when(passwordRecoveryService).verifyCode("enzo@email.com", "000000");

        mockMvc.perform(post("/auth/verify-recovery-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "enzo@email.com",
                                  "codigo": "000000"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Código de recuperação inválido ou expirado"));
    }

    @Test
    void changePasswordReturns204() throws Exception {
        doNothing().when(userService).changePassword(any(User.class), org.mockito.ArgumentMatchers.eq("senha"),
                org.mockito.ArgumentMatchers.eq("novaSenha"), org.mockito.ArgumentMatchers.eq("novaSenha"));

        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "senha",
                                  "novaSenha": "novaSenha",
                                  "confirmacaoSenha": "novaSenha"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void changePasswordReturns401WhenCurrentPasswordIsWrong() throws Exception {
        doThrow(new BadCredentialsException("Senha atual incorreta"))
                .when(userService).changePassword(any(User.class), org.mockito.ArgumentMatchers.eq("errada"),
                        org.mockito.ArgumentMatchers.eq("novaSenha"), org.mockito.ArgumentMatchers.eq("novaSenha"));

        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "errada",
                                  "novaSenha": "novaSenha",
                                  "confirmacaoSenha": "novaSenha"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Senha atual incorreta"));
    }

    @Test
    void changePasswordReturns400WhenConfirmationDoesNotMatch() throws Exception {
        doThrow(new PasswordConfirmationMismatchException())
                .when(userService).changePassword(any(User.class), org.mockito.ArgumentMatchers.eq("senha"),
                        org.mockito.ArgumentMatchers.eq("novaSenha"), org.mockito.ArgumentMatchers.eq("outraSenha"));

        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "senhaAtual": "senha",
                                  "novaSenha": "novaSenha",
                                  "confirmacaoSenha": "outraSenha"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A confirmação da senha não confere"));
    }

    private HandlerMethodArgumentResolver authenticatedUserResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return org.mockito.Mockito.mock(User.class);
            }
        };
    }
}
