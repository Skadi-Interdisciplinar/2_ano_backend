package skadi.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.AdminListResponse;
import skadi.api.dto.UserResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.UserAlreadyExistsException;
import skadi.api.exceptions.UserNotFoundException;
import skadi.api.model.DistributionCenter;
import skadi.api.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SuperAdminControllerTest {

    @Mock
    private UserService userService;

    private static final String SUPER_ADMIN_BEARER = "Bearer eyJhbGciOiJIUzI1NiJ9.admin-token";
    private static final String ADMIN_BODY = """
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
            """;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SuperAdminController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAdmins_returns200WithAdminList() throws Exception {
        DistributionCenter cd = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        UserResponse admin = new UserResponse(10L, "Enzo", "enzo", "enzo@email.com", NivelAcesso.ADMIN, List.of(cd));
        when(userService.getAdminsSvc()).thenReturn(new AdminListResponse(1, List.of(admin)));

        mockMvc.perform(get("/super_admin/get_admins")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.usuarios[0].id").value(10))
                .andExpect(jsonPath("$.usuarios[0].nome").value("Enzo"))
                .andExpect(jsonPath("$.usuarios[0].username").value("enzo"))
                .andExpect(jsonPath("$.usuarios[0].email").value("enzo@email.com"))
                .andExpect(jsonPath("$.usuarios[0].nivelAcesso").value("ADMIN"))
                .andExpect(jsonPath("$.usuarios[0].cd[0].id").value(1))
                .andExpect(jsonPath("$.usuarios[0].cd[0].name").value("CD Osasco"))
                .andExpect(jsonPath("$.usuarios[0].cd[0].cnpj").value("12345678901234"));
    }

    @Test
    void getAdmins_returns200WithEmptyList() throws Exception {
        when(userService.getAdminsSvc()).thenReturn(new AdminListResponse(0, List.of()));

        mockMvc.perform(get("/super_admin/get_admins")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.usuarios").isArray())
                .andExpect(jsonPath("$.usuarios").isEmpty());
    }

    @Test
    void createAdmin_returns201() throws Exception {
        DistributionCenter cd = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        when(userService.createAdmin(any())).thenReturn(
                new UserResponse(10L, "Enzo", "enzo", "enzo@email.com", NivelAcesso.ADMIN, List.of(cd))
        );

        mockMvc.perform(post("/super_admin/admins")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.username").value("enzo"))
                .andExpect(jsonPath("$.nivelAcesso").value("ADMIN"))
                .andExpect(jsonPath("$.cd[0].name").value("CD Osasco"));
    }

    @Test
    void createAdmin_returns403WhenRoleIsNotAdmin() throws Exception {
        when(userService.createAdmin(any()))
                .thenThrow(new AccessDeniedException("SUPER_ADMIN cadastra apenas ADMIN"));

        mockMvc.perform(post("/super_admin/admins")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("SUPER_ADMIN cadastra apenas ADMIN"));
    }

    @Test
    void createAdmin_returns409WhenUsernameExists() throws Exception {
        when(userService.createAdmin(any())).thenThrow(new UserAlreadyExistsException());

        mockMvc.perform(post("/super_admin/admins")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("Nome de usuário já utilizado"));
    }

    @Test
    void updateAdmin_returns200() throws Exception {
        DistributionCenter cd = new DistributionCenter(1L, "CD Osasco", "12345678901234", null);
        when(userService.updateAdmin(eq(10L), any())).thenReturn(
                new UserResponse(10L, "Enzo Atualizado", "enzo", "enzo@email.com", NivelAcesso.ADMIN, List.of(cd))
        );

        mockMvc.perform(put("/super_admin/admins/10")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nome").value("Enzo Atualizado"));
    }

    @Test
    void updateAdmin_returns404WhenAdminDoesNotExist() throws Exception {
        when(userService.updateAdmin(eq(99L), any())).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(put("/super_admin/admins/99")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Usuário não encontrado: 99"));
    }

    @Test
    void deleteAdmin_returns204() throws Exception {
        doNothing().when(userService).deleteAdmin(10L);

        mockMvc.perform(delete("/super_admin/admins/10")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAdmin_returns404WhenAdminDoesNotExist() throws Exception {
        doThrow(new UserNotFoundException(99L)).when(userService).deleteAdmin(99L);

        mockMvc.perform(delete("/super_admin/admins/99")
                        .header(HttpHeaders.AUTHORIZATION, SUPER_ADMIN_BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Usuário não encontrado: 99"));
    }
}
