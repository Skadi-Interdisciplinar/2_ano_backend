package skadi.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
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
import skadi.api.service.AlertService;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                "email@test.com", "senha", NivelAcesso.OPERADOR, 1, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAll_returnsAlerts() throws Exception {
        when(service.findAll(null)).thenReturn(List.of(alert(AlertStatus.ATIVO)));

        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("ATIVO"));
    }

    @Test
    void findAll_acceptsColdRoomFilter() throws Exception {
        when(service.findAll(3)).thenReturn(List.of(alert(AlertStatus.RECONHECIDO)));

        mockMvc.perform(get("/alerts").param("coldRoomId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].coldRoomId").value(3));
    }

    @Test
    void findHistory_returnsResolvedAlerts() throws Exception {
        when(service.findHistory(null)).thenReturn(List.of(alert(AlertStatus.RESOLVIDO)));

        mockMvc.perform(get("/alerts/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RESOLVIDO"));
    }

    @Test
    void findHistory_acceptsColdRoomFilter() throws Exception {
        when(service.findHistory(3)).thenReturn(List.of(alert(AlertStatus.RESOLVIDO)));

        mockMvc.perform(get("/alerts/history").param("coldRoomId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].coldRoomId").value(3));
    }

    @Test
    void findById_returnsAlert() throws Exception {
        when(service.findById(1)).thenReturn(alert(AlertStatus.ATIVO));

        mockMvc.perform(get("/alerts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_doesNotReceiveLevelOrSeverityInRequest() throws Exception {
        when(service.update(eq(1), any())).thenReturn(alert(AlertStatus.ATIVO));

        mockMvc.perform(put("/alerts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coldRoomId": 3,
                                  "referenceShelfLifeHours": 24.00,
                                  "type": "temperatura_fora_padrao"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void recognize_returnsNoContent() throws Exception {
        doNothing().when(service).recognize(1, 7);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        mockMvc.perform(patch("/alerts/1/recognize")
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        doNothing().when(service).delete(1);

        mockMvc.perform(delete("/alerts/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void update_returnsBadRequestWhenRequiredFieldIsMissing() throws Exception {
        mockMvc.perform(put("/alerts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "referenceShelfLifeHours": 24.00,
                                  "type": "temperatura_fora_padrao"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private AlertResponse alert(AlertStatus status) {
        return new AlertResponse(1, 3, new BigDecimal("24.00"), CurrentLevel.OPERADOR,
                LocalDateTime.of(2026, 10, 5, 10, 0),
                "temperatura_fora_padrao", SeverityLevel.BAIXA, status);
    }
}
