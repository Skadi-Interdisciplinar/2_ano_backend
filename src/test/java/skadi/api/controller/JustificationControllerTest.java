package skadi.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.JustificationRequest;
import skadi.api.dto.JustificationPatchRequest;
import skadi.api.dto.JustificationResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.JustificationAlreadyExistsException;
import skadi.api.exceptions.JustificationNotFoundException;
import skadi.api.exceptions.JustificationRuleViolationException;
import skadi.api.model.User;
import skadi.api.service.JustificationService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JustificationControllerTest {
    @Mock private JustificationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new JustificationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                        return new User(7L, "Test User", "test-user", "12345678901",
                                "test@example.com", "password", NivelAcesso.ADMIN, 1, null);
                    }
                })
                .build();
    }

    @Test
    void findByAlertHandlingId_returnsJustification() throws Exception {
        when(service.findByAlertHandlingId(5)).thenReturn(justification());

        mockMvc.perform(get("/alert-handlings/5/justification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.alertHandlingId").value(5))
                .andExpect(jsonPath("$.reason").value("Power failure"));
    }

    @Test
    void findByAlertHandlingId_returnsNotFoundWhenNoneExists() throws Exception {
        when(service.findByAlertHandlingId(5)).thenThrow(new JustificationNotFoundException(5));

        mockMvc.perform(get("/alert-handlings/5/justification"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreatedJustification() throws Exception {
        when(service.create(eq(5), any(JustificationRequest.class), eq(7)))
                .thenReturn(justification());

        mockMvc.perform(post("/alert-handlings/5/justification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "Power failure", "description": "Temporary outage" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.alertHandlingId").value(5))
                .andExpect(jsonPath("$.reason").value("Power failure"));
    }

    @Test
    void create_returnsConflictWhenHandlingAlreadyHasJustification() throws Exception {
        when(service.create(eq(5), any(JustificationRequest.class), eq(7)))
                .thenThrow(new JustificationAlreadyExistsException(5));

        mockMvc.perform(post("/alert-handlings/5/justification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "Motivo", "description": "Descrição" }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void create_returnsBadRequestWhenReasonIsBlank() throws Exception {
        mockMvc.perform(post("/alert-handlings/5/justification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "  ", "description": "Temporary outage" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsBadRequestWhenDescriptionIsBlank() throws Exception {
        mockMvc.perform(post("/alert-handlings/5/justification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "Power failure", "description": " " }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsBadRequestWhenReasonExceedsDatabaseLimit() throws Exception {
        String tooLongReason = "r".repeat(256);
        String body = "{\"reason\":\"" + tooLongReason
                + "\",\"description\":\"Temporary outage\"}";

        mockMvc.perform(post("/alert-handlings/5/justification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_changesJustification() throws Exception {
        JustificationResponse updated = new JustificationResponse(20, 5,
                "New reason", "New description", justification().createdAt());
        when(service.update(eq(5), any(JustificationPatchRequest.class), eq(7)))
                .thenReturn(updated);

        mockMvc.perform(patch("/alert-handlings/5/justification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "New reason", "description": "New description" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("New reason"))
                .andExpect(jsonPath("$.description").value("New description"));
    }

    @Test
    void update_acceptsPartialRequest() throws Exception {
        when(service.update(eq(5), any(JustificationPatchRequest.class), eq(7)))
                .thenReturn(new JustificationResponse(20, 5,
                        "New reason", "Existing description", justification().createdAt()));

        mockMvc.perform(patch("/alert-handlings/5/justification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "New reason" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("New reason"))
                .andExpect(jsonPath("$.description").value("Existing description"));
    }

    @Test
    void update_returnsConflictWhenNoFieldsAreProvided() throws Exception {
        when(service.update(eq(5), any(JustificationPatchRequest.class), eq(7)))
                .thenThrow(new JustificationRuleViolationException("Informe ao menos um campo para atualizar."));

        mockMvc.perform(patch("/alert-handlings/5/justification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void update_returnsBadRequestWhenProvidedFieldIsBlank() throws Exception {
        mockMvc.perform(patch("/alert-handlings/5/justification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "description": " " }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returnsNotFoundWhenHandlingHasNoJustification() throws Exception {
        when(service.update(eq(5), any(JustificationPatchRequest.class), eq(7)))
                .thenThrow(new JustificationNotFoundException(5));

        mockMvc.perform(patch("/alert-handlings/5/justification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "Reason", "description": "Description" }
                                """))
                .andExpect(status().isNotFound());
    }

    private JustificationResponse justification() {
        return new JustificationResponse(20, 5, "Power failure", "Temporary outage",
                LocalDateTime.of(2026, 10, 7, 10, 0));
    }
}
