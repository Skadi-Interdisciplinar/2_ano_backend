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
import skadi.api.dto.JustificationRequest;
import skadi.api.dto.JustificationResponse;
import skadi.api.service.JustificationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .build();
    }

    @Test
    void findByAlertHandlingId_returnsJustifications() throws Exception {
        when(service.findByAlertHandlingId(5)).thenReturn(List.of(justification()));

        mockMvc.perform(get("/alert-handlings/5/justifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].alertHandlingId").value(5))
                .andExpect(jsonPath("$[0].reason").value("Power failure"));
    }

    @Test
    void findByAlertHandlingId_returnsEmptyListWhenNoneExist() throws Exception {
        when(service.findByAlertHandlingId(5)).thenReturn(List.of());

        mockMvc.perform(get("/alert-handlings/5/justifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void create_returnsCreatedJustification() throws Exception {
        when(service.create(eq(5), any(JustificationRequest.class))).thenReturn(justification());

        mockMvc.perform(post("/alert-handlings/5/justifications")
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
    void create_returnsBadRequestWhenReasonIsBlank() throws Exception {
        mockMvc.perform(post("/alert-handlings/5/justifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "  ", "description": "Temporary outage" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsBadRequestWhenDescriptionIsBlank() throws Exception {
        mockMvc.perform(post("/alert-handlings/5/justifications")
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

        mockMvc.perform(post("/alert-handlings/5/justifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_returnsJustificationForHandling() throws Exception {
        when(service.findById(5, 20)).thenReturn(justification());

        mockMvc.perform(get("/alert-handlings/5/justifications/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.alertHandlingId").value(5));
    }

    @Test
    void findById_returnsInternalServerErrorWhenNotFoundForHandling() throws Exception {
        when(service.findById(5, 99)).thenThrow(
                new IllegalArgumentException("Justification not found for alert handling: 5"));

        mockMvc.perform(get("/alert-handlings/5/justifications/99"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void update_changesJustification() throws Exception {
        JustificationResponse updated = new JustificationResponse(20, 5,
                "New reason", "New description", justification().createdAt());
        when(service.update(eq(5), eq(20), any(JustificationRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/alert-handlings/5/justifications/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "New reason", "description": "New description" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("New reason"))
                .andExpect(jsonPath("$.description").value("New description"));
    }

    @Test
    void update_returnsBadRequestWhenRequiredFieldIsMissing() throws Exception {
        mockMvc.perform(put("/alert-handlings/5/justifications/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "Reason only" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returnsInternalServerErrorWhenNotFoundForHandling() throws Exception {
        when(service.update(eq(5), eq(99), any(JustificationRequest.class)))
                .thenThrow(new IllegalArgumentException("Justification not found for alert handling: 5"));

        mockMvc.perform(put("/alert-handlings/5/justifications/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "reason": "Reason", "description": "Description" }
                                """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        doNothing().when(service).delete(5, 20);

        mockMvc.perform(delete("/alert-handlings/5/justifications/20"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returnsInternalServerErrorWhenNotFoundForHandling() throws Exception {
        org.mockito.Mockito.doThrow(
                        new IllegalArgumentException("Justification not found for alert handling: 5"))
                .when(service).delete(5, 99);

        mockMvc.perform(delete("/alert-handlings/5/justifications/99"))
                .andExpect(status().isInternalServerError());
    }

    private JustificationResponse justification() {
        return new JustificationResponse(20, 5, "Power failure", "Temporary outage",
                LocalDateTime.of(2026, 10, 7, 10, 0));
    }
}
