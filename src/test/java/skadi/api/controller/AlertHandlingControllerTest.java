package skadi.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.AlertHandlingResponse;
import skadi.api.dto.AlertHandlingUpdateRequest;
import skadi.api.enums.AlertHandlingStatus;
import skadi.api.service.AlertHandlingService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .build();
    }

    @Test
    void findByAlertId_returnsHandlings() throws Exception {
        when(service.findByAlertId(3)).thenReturn(List.of(handling(AlertHandlingStatus.PENDENTE)));

        mockMvc.perform(get("/alerts/3/alert-handlings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].alertId").value(3))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"));
    }

    @Test
    void findByAlertId_returnsEmptyListWhenNoHandlingsExist() throws Exception {
        when(service.findByAlertId(3)).thenReturn(List.of());

        mockMvc.perform(get("/alerts/3/alert-handlings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findById_returnsHandling() throws Exception {
        when(service.findById(3, 10)).thenReturn(handling(AlertHandlingStatus.EM_ANDAMENTO));

        mockMvc.perform(get("/alerts/3/alert-handlings/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    void findById_returnsInternalServerErrorWhenHandlingDoesNotExist() throws Exception {
        when(service.findById(3, 99)).thenThrow(
                new IllegalArgumentException("Alert handling not found for alert: 3"));

        mockMvc.perform(get("/alerts/3/alert-handlings/99"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void update_changesStatus() throws Exception {
        when(service.update(eq(3), eq(10), any(AlertHandlingUpdateRequest.class)))
                .thenReturn(handling(AlertHandlingStatus.EM_ANDAMENTO));

        mockMvc.perform(put("/alerts/3/alert-handlings/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "EM_ANDAMENTO" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    void update_returnsBadRequestWhenStatusIsNull() throws Exception {
        mockMvc.perform(put("/alerts/3/alert-handlings/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": null }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_currentlyReturnsInternalServerErrorWhenStatusIsInvalid() throws Exception {
        mockMvc.perform(put("/alerts/3/alert-handlings/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "INVALIDO" }
                                """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        doNothing().when(service).delete(3, 10);

        mockMvc.perform(delete("/alerts/3/alert-handlings/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsInternalServerErrorWhenHandlingDoesNotExist() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Alert handling not found for alert: 3"))
                .when(service).delete(3, 99);

        mockMvc.perform(delete("/alerts/3/alert-handlings/99"))
                .andExpect(status().isInternalServerError());
    }

    private AlertHandlingResponse handling(AlertHandlingStatus status) {
        return new AlertHandlingResponse(10, 3, 7L,
                LocalDateTime.of(2026, 10, 5, 10, 0),
                LocalDateTime.of(2026, 10, 5, 11, 0), status);
    }
}
