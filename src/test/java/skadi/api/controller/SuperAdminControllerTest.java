package skadi.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.DistributionCenterResponse;
import skadi.api.dto.SupportRequestResponse;
import skadi.api.dto.SupportRequestUpdateRequest;
import skadi.api.dto.UserRequest;
import skadi.api.dto.UserResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.enums.SupportCategory;
import skadi.api.enums.SupportStatus;
import skadi.api.exceptions.AccessLevelChangeNotAllowedException;
import skadi.api.exceptions.SupportRequestNotFoundException;
import skadi.api.exceptions.UserNotFoundException;
import skadi.api.service.SuperAdminService;
import skadi.api.service.SupportRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SuperAdminControllerTest {

    @Mock
    private SuperAdminService service;

    @Mock
    private SupportRequestService supportRequestService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SuperAdminController(service, supportRequestService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateReturnsUpdatedUser() throws Exception {
        when(service.update(eq(3L), any(UserRequest.class))).thenReturn(response());

        mockMvc.perform(put("/super_admin/employees_admin/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.nome").value("Enzo Nascimento"))
                .andExpect(jsonPath("$.username").value("enzo.n"))
                .andExpect(jsonPath("$.email").value("novo@email.com"))
                .andExpect(jsonPath("$.nivelAcesso").value("ADMIN"))
                .andExpect(jsonPath("$.codGestor").value(10));

        ArgumentCaptor<UserRequest> captor = ArgumentCaptor.forClass(UserRequest.class);
        verify(service).update(eq(3L), captor.capture());
        assertEquals("Enzo Nascimento", captor.getValue().nome());
        assertEquals(NivelAcesso.ADMIN, captor.getValue().nivelAcesso());
        assertEquals(10, captor.getValue().codGestor());
    }

    @Test
    void updateReturns400WhenAccessLevelChanges() throws Exception {
        when(service.update(eq(3L), any(UserRequest.class)))
                .thenThrow(new AccessLevelChangeNotAllowedException());

        mockMvc.perform(put("/super_admin/employees_admin/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("GESTOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Não é permitido alterar o nível de acesso"));
    }

    @Test
    void updateReturns404WhenUserDoesNotExist() throws Exception {
        when(service.update(eq(99L), any(UserRequest.class)))
                .thenThrow(new UserNotFoundException("Usuário não encontrado"));

        mockMvc.perform(put("/super_admin/employees_admin/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Usuário não encontrado"));
    }

    @Test
    void deleteReturns204() throws Exception {
        doNothing().when(service).delete(3L);

        mockMvc.perform(delete("/super_admin/employees_admin/3"))
                .andExpect(status().isNoContent());

        verify(service).delete(3L);
    }

    @Test
    void deleteReturns404WhenUserDoesNotExist() throws Exception {
        doThrow(new UserNotFoundException("Usuário não encontrado")).when(service).delete(99L);

        mockMvc.perform(delete("/super_admin/employees_admin/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Usuário não encontrado"));
    }

    @Test
    void findAllSupportRequestsReturnsTickets() throws Exception {
        when(supportRequestService.findAll()).thenReturn(List.of(ticket()));

        mockMvc.perform(get("/super_admin/support"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("REGISTRADO"))
                .andExpect(jsonPath("$[0].categoria").value("DUVIDA"));
    }

    @Test
    void findSupportRequestByIdReturnsTicket() throws Exception {
        when(supportRequestService.findById(1L)).thenReturn(ticket());

        mockMvc.perform(get("/super_admin/support/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.codUsuarioSolicitante").value(4))
                .andExpect(jsonPath("$.status").value("REGISTRADO"))
                .andExpect(jsonPath("$.categoria").value("DUVIDA"));
    }

    @Test
    void findSupportRequestByIdReturns404WhenTicketDoesNotExist() throws Exception {
        when(supportRequestService.findById(99L)).thenThrow(new SupportRequestNotFoundException());

        mockMvc.perform(get("/super_admin/support/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Chamado não encontrado"));
    }

    @Test
    void updateSupportRequestReturnsUpdatedTicket() throws Exception {
        when(supportRequestService.update(eq(1L), any(SupportRequestUpdateRequest.class), any()))
                .thenReturn(ticket());

        mockMvc.perform(put("/super_admin/support/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoria": "RECLAMACAO",
                                  "descricao": "O relatório continua indisponível.",
                                  "resposta": "Estamos verificando."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.descricao").value("Não consigo abrir o relatório."));

        ArgumentCaptor<SupportRequestUpdateRequest> captor = ArgumentCaptor.forClass(SupportRequestUpdateRequest.class);
        verify(supportRequestService).update(eq(1L), captor.capture(), any());
        assertEquals(SupportCategory.RECLAMACAO, captor.getValue().categoria());
        assertEquals("Estamos verificando.", captor.getValue().resposta());
    }

    @Test
    void updateSupportRequestReturns404WhenTicketDoesNotExist() throws Exception {
        when(supportRequestService.update(eq(99L), any(SupportRequestUpdateRequest.class), any()))
                .thenThrow(new SupportRequestNotFoundException());

        mockMvc.perform(put("/super_admin/support/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoria": "RECLAMACAO",
                                  "descricao": "O relatório continua indisponível.",
                                  "resposta": "Estamos verificando."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Chamado não encontrado"));
    }

    @Test
    void updateSupportRequestStatusForwardsOnlyStatus() throws Exception {
        when(supportRequestService.updateStatus(eq(1L), eq(SupportStatus.ATENDIDO), any())).thenReturn(ticket());

        mockMvc.perform(patch("/super_admin/support/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "ATENDIDO" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGISTRADO"));

        verify(supportRequestService).updateStatus(eq(1L), eq(SupportStatus.ATENDIDO), any());
    }

    @Test
    void updateSupportRequestStatusReturns404WhenTicketDoesNotExist() throws Exception {
        when(supportRequestService.updateStatus(eq(99L), any(SupportStatus.class), any()))
                .thenThrow(new SupportRequestNotFoundException());

        mockMvc.perform(patch("/super_admin/support/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "ATENDIDO" }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Chamado não encontrado"));
    }

    @Test
    void deleteSupportRequestReturns204() throws Exception {
        doNothing().when(supportRequestService).delete(1L);

        mockMvc.perform(delete("/super_admin/support/1"))
                .andExpect(status().isNoContent());

        verify(supportRequestService).delete(1L);
    }

    @Test
    void deleteSupportRequestReturns404WhenTicketDoesNotExist() throws Exception {
        doThrow(new SupportRequestNotFoundException()).when(supportRequestService).delete(99L);

        mockMvc.perform(delete("/super_admin/support/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Chamado não encontrado"));
    }

    private SupportRequestResponse ticket() {
        return new SupportRequestResponse(
                1L,
                4L,
                SupportCategory.DUVIDA,
                "Não consigo abrir o relatório.",
                SupportStatus.REGISTRADO,
                LocalDateTime.of(2026, 10, 29, 8, 25),
                null,
                null,
                null
        );
    }

    private UserResponse response() {
        return new UserResponse(
                3L,
                "Enzo Nascimento",
                "enzo.n",
                "10987654321",
                "novo@email.com",
                NivelAcesso.ADMIN,
                null,
                List.of(new DistributionCenterResponse(7L, "CD 7", "12345678901234")),
                10
        );
    }

    private String validRequest(String nivelAcesso) {
        return """
                {
                  "id": 3,
                  "nome": "Enzo Nascimento",
                  "username": "enzo.n",
                  "cpf": "10987654321",
                  "email": "novo@email.com",
                  "nivelAcesso": "%s",
                  "cdGestor": null,
                  "cd": null,
                  "codGestor": 10
                }
                """.formatted(nivelAcesso);
    }
}
