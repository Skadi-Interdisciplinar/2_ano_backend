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
import skadi.api.dto.UserRequest;
import skadi.api.dto.UserResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.AccessLevelChangeNotAllowedException;
import skadi.api.exceptions.UserNotFoundException;
import skadi.api.service.SuperAdminService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SuperAdminControllerTest {

    @Mock
    private SuperAdminService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SuperAdminController(service))
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
