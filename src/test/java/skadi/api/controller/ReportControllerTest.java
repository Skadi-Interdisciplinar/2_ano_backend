package skadi.api.controller;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.ReportRequest;
import skadi.api.dto.ReportPatchRequest;
import skadi.api.dto.ReportResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.enums.ReportStatus;
import skadi.api.exceptions.ReportNotFoundException;
import skadi.api.exceptions.ReportRuleViolationException;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;
import skadi.api.service.ReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock private ReportService service;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User(7L, "Test User", "testuser", "12345678901",
                "test@example.com", "password", NivelAcesso.GESTOR,
                new DistributionCenter(1L, "CD de teste", "12345678901234"), null);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        return user;
                    }
                })
                .build();
    }

    @AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void create_returnsCreatedReportAndUsesAuthenticatedUserAsAuthor() throws Exception {
        when(service.create(any(ReportRequest.class), eq(user))).thenReturn(response());

        mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.generatedByUserId").value(7))
                .andExpect(jsonPath("$.status").value("GERADO"));

        verify(service).create(any(ReportRequest.class), eq(user));
    }

    @Test
    void create_returnsBadRequestWhenContentIsBlank() throws Exception {
        mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": " ",
                                  "periodStart": "2026-09-01",
                                  "periodEnd": "2026-09-30",
                                  "distributionCenterId": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void create_returnsBadRequestWhenPeriodIsMissing() throws Exception {
        mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Relatório de teste",
                                  "periodStart": null,
                                  "periodEnd": "2026-09-30",
                                  "distributionCenterId": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void findAll_returnsReports() throws Exception {
        when(service.findAll(user)).thenReturn(List.of(response()));

        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].contentHash").value("hash-de-teste"));

        verify(service).findAll(user);
    }

    @Test
    void findAll_returnsEmptyListWhenThereAreNoReports() throws Exception {
        when(service.findAll(user)).thenReturn(List.of());

        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findById_returnsReport() throws Exception {
        when(service.findById(12, user)).thenReturn(response());

        mockMvc.perform(get("/reports/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.content").value("Relatório de teste"));

        verify(service).findById(12, user);
    }

    @Test
    void findById_returnsNotFoundWhenReportDoesNotExist() throws Exception {
        when(service.findById(99, user)).thenThrow(new ReportNotFoundException(99));

        mockMvc.perform(get("/reports/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returnsUpdatedReportForAuthenticatedAuthor() throws Exception {
        when(service.update(eq(12), any(ReportPatchRequest.class), eq(user))).thenReturn(response());

        mockMvc.perform(patch("/reports/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Conteúdo alterado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12));

        verify(service).update(eq(12), any(ReportPatchRequest.class), eq(user));
    }

    @Test
    void update_returnsConflictWhenPeriodIsInvalid() throws Exception {
        when(service.update(eq(12), any(ReportPatchRequest.class), eq(user)) )
                .thenThrow(new ReportRuleViolationException("Período inválido"));

        mockMvc.perform(patch("/reports/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Relatório",
                                  "periodStart": "2026-10-01",
                                  "periodEnd": "2026-09-01"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void update_acceptsPartialPatchBody() throws Exception {
        when(service.update(eq(12), any(ReportPatchRequest.class), eq(user))).thenReturn(response());

        mockMvc.perform(patch("/reports/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Somente conteúdo atualizado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12));

        verify(service).update(eq(12), any(ReportPatchRequest.class), eq(user));
    }

    @Test
    void delete_returnsNoContentForValidRequest() throws Exception {
        mockMvc.perform(delete("/reports/12"))
                .andExpect(status().isNoContent());

        verify(service).delete(12, user);
    }

    @Test
    void delete_returnsConflictWhenReportCannotBeDeleted() throws Exception {
        org.mockito.Mockito.doThrow(new ReportRuleViolationException("Relatório assinado"))
                .when(service).delete(12, user);

        mockMvc.perform(delete("/reports/12"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStatus_returnsUpdatedStatus() throws Exception {
        when(service.updateStatus(12, ReportStatus.ARQUIVADO, user)).thenReturn(
                new ReportResponse(12, 7, 1, response().generatedAt(), "hash-de-teste",
                        response().periodStart(), response().periodEnd(), ReportStatus.ARQUIVADO,
                        "Relatório de teste"));

        mockMvc.perform(patch("/reports/12/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARQUIVADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARQUIVADO"));

        verify(service).updateStatus(12, ReportStatus.ARQUIVADO, user);
    }

    @Test
    void updateStatus_returnsBadRequestWhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/reports/12/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":null}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void updateStatus_returnsConflictWhenTransitionIsNotAllowed() throws Exception {
        when(service.updateStatus(12, ReportStatus.GERADO, user))
                .thenThrow(new ReportRuleViolationException("Transição não permitida"));

        mockMvc.perform(patch("/reports/12/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GERADO\"}"))
                .andExpect(status().isConflict());
    }

    private String requestJson() {
        return """
                {
                  "content": "Relatório de teste",
                  "periodStart": "2026-09-01",
                  "periodEnd": "2026-09-30",
                  "distributionCenterId": 1
                }
                """;
    }

    private ReportResponse response() {
        return new ReportResponse(12, 7, 1,
                LocalDateTime.of(2026, 10, 10, 12, 0), "hash-de-teste",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                ReportStatus.GERADO, "Relatório de teste");
    }
}
