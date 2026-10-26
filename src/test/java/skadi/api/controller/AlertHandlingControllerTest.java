package skadi.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.AlertHandlingResponse;
import skadi.api.enums.AlertHandlingStatus;
import skadi.api.exceptions.AlertHandlingNotFoundException;
import skadi.api.service.AlertHandlingService;
import skadi.api.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AlertHandlingControllerTest {
    @Mock private AlertHandlingService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AlertHandlingController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authenticatedUserResolver())
                .build();
    }

    @Test
    void findByAlertId_returnsSingleHandling() throws Exception {
        when(service.findByAlertId(org.mockito.ArgumentMatchers.eq(3), any(User.class)))
                .thenReturn(handling(AlertHandlingStatus.PENDENTE));

        mockMvc.perform(get("/alerts/3/alert-handling"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.alertId").value(3))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void findByAlertId_returnsNotFoundWhenHandlingDoesNotExist() throws Exception {
        when(service.findByAlertId(org.mockito.ArgumentMatchers.eq(99), any(User.class))).thenThrow(
                AlertHandlingNotFoundException.forAlertId(99));

        mockMvc.perform(get("/alerts/99/alert-handling"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Atendimento não encontrado para o alerta: 99"));
    }

    @Test
    void findAll_returnsHandlings() throws Exception {
        when(service.findAll(any(User.class))).thenReturn(List.of(handling(AlertHandlingStatus.PENDENTE)));

        mockMvc.perform(get("/alert-handlings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].alertId").value(3))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"));
    }

    @Test
    void findById_returnsHandling() throws Exception {
        when(service.findById(org.mockito.ArgumentMatchers.eq(10), any(User.class)))
                .thenReturn(handling(AlertHandlingStatus.EM_ANDAMENTO));

        mockMvc.perform(get("/alert-handlings/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    void findById_returnsNotFoundWhenHandlingDoesNotExist() throws Exception {
        when(service.findById(org.mockito.ArgumentMatchers.eq(99), any(User.class))).thenThrow(
                AlertHandlingNotFoundException.forHandlingId(99));

        mockMvc.perform(get("/alert-handlings/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Atendimento não encontrado: 99"));
    }

    private AlertHandlingResponse handling(AlertHandlingStatus status) {
        return new AlertHandlingResponse(10, 3, 7L,
                LocalDateTime.of(2026, 10, 5, 10, 0),
                LocalDateTime.of(2026, 10, 5, 11, 0), status);
    }

    private HandlerMethodArgumentResolver authenticatedUserResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                return org.mockito.Mockito.mock(User.class);
            }
        };
    }
}
