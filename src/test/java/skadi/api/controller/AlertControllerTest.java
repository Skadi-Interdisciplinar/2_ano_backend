package skadi.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.AlertResponse;
import skadi.api.enums.AlertStatus;
import skadi.api.enums.CurrentLevel;
import skadi.api.enums.SeverityLevel;
import skadi.api.exceptions.AlertNotFoundException;
import skadi.api.service.AlertService;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.User;
import skadi.api.model.DistributionCenter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {
    @Mock private AlertService service;
    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AlertController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                   ModelAndViewContainer mavContainer,
                                                   NativeWebRequest webRequest,
                                                   org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                        return user;
                    }
                })
                .build();
        user = new User(7L, "Operator", "operator", "12345678901",
                "email@test.com", "senha", NivelAcesso.OPERADOR,
                new DistributionCenter(1L, "CD de teste", "12345678901234", null), null, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAll_returnsAlerts() throws Exception {
        when(service.findAll(org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(List.of(alert(AlertStatus.ATIVO)));

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("ATIVO"));
    }

    @Test
    void findAll_acceptsColdRoomFilter() throws Exception {
        when(service.findAll(eq(3), any())).thenReturn(List.of(alert(AlertStatus.RECONHECIDO)));

        mockMvc.perform(get("/alerts").param("coldRoomId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].coldRoomId").value(3));
    }

    @Test
    void findHistory_returnsResolvedAlerts() throws Exception {
        when(service.findHistory(org.mockito.ArgumentMatchers.isNull(), any())).thenReturn(List.of(alert(AlertStatus.RESOLVIDO)));

        mockMvc.perform(get("/alerts/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RESOLVIDO"));
    }

    @Test
    void findHistory_acceptsColdRoomFilter() throws Exception {
        when(service.findHistory(eq(3), any())).thenReturn(List.of(alert(AlertStatus.RESOLVIDO)));

        mockMvc.perform(get("/alerts/history").param("coldRoomId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].coldRoomId").value(3));
    }

    @Test
    void findById_returnsAlert() throws Exception {
        when(service.findById(eq(1), any())).thenReturn(alert(AlertStatus.ATIVO));

        mockMvc.perform(get("/alerts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findById_returnsNotFoundWhenAlertDoesNotExist() throws Exception {
        when(service.findById(eq(99), any())).thenThrow(new AlertNotFoundException(99));

        mockMvc.perform(get("/alerts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Alerta não encontrado: 99"));
    }

    @Test
    void recognize_returnsNoContent() throws Exception {
        doNothing().when(service).recognize(1, user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        mockMvc.perform(patch("/alerts/1/recognize")
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void recognize_returnsForbiddenWhenUserCannotRecognizeAlert() throws Exception {
        String message = "Somente usuários vinculados ao CD do alerta podem reconhecê-lo.";
        org.mockito.Mockito.doThrow(new AccessDeniedException(message))
                .when(service).recognize(1, user);

        mockMvc.perform(patch("/alerts/1/recognize"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(message));
    }

    private AlertResponse alert(AlertStatus status) {
        return new AlertResponse(1, 3, new BigDecimal("24.00"), CurrentLevel.OPERADOR,
                LocalDateTime.of(2026, 10, 5, 10, 0),
                "temperatura_fora_padrao", SeverityLevel.BAIXA, status);
    }
}
