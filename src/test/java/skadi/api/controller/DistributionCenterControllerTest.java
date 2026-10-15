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
import skadi.api.dto.DistributionCenterRequestName;
import skadi.api.dto.DistributionCenterResponse;
import skadi.api.exceptions.CdNotFoundException;
import skadi.api.service.DistributionCenterService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DistributionCenterControllerTest {

    @Mock
    private DistributionCenterService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DistributionCenterController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void insertCd_returns201() throws Exception {
        when(service.insertCd(any())).thenReturn(new DistributionCenterResponse(1L, "CD Osasco", "12345678901234"));

        mockMvc.perform(post("/cd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "CD Osasco", "cnpj": "12345678901234" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("CD Osasco"))
                .andExpect(jsonPath("$.cnpj").value("12345678901234"));
    }

    @Test
    void findAllCD_returns200() throws Exception {
        when(service.findAllCD()).thenReturn(List.of(
                new DistributionCenterResponse(1L, "CD Osasco", "12345678901234")
        ));

        mockMvc.perform(get("/cd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("CD Osasco"));
    }

    @Test
    void findCDById_returns200() throws Exception {
        when(service.findCDById(1L)).thenReturn(
                new DistributionCenterResponse(1L, "CD Osasco", "12345678901234")
        );

        mockMvc.perform(get("/cd/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("CD Osasco"));
    }

    @Test
    void findCDById_returns404WhenMissing() throws Exception {
        when(service.findCDById(99L)).thenThrow(new CdNotFoundException());

        mockMvc.perform(get("/cd/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Centro de distribuição não encontrado"));
    }

    @Test
    void updateCD_returns200() throws Exception {
        when(service.updateCD(any(), eq(1L))).thenReturn(
                new DistributionCenterResponse(1L, "CD Barueri", "12345678901234")
        );

        mockMvc.perform(put("/cd/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "CD Barueri", "cnpj": "12345678901234" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CD Barueri"));
    }

    @Test
    void updateCD_returns404WhenMissing() throws Exception {
        when(service.updateCD(any(), eq(99L))).thenThrow(new CdNotFoundException());

        mockMvc.perform(put("/cd/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "CD Barueri", "cnpj": "12345678901234" }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    void updateNameCd_returns200() throws Exception {
        when(service.updateNameCd(any(DistributionCenterRequestName.class), eq(1L))).thenReturn(
                new DistributionCenterResponse(1L, "CD Atualizado", "12345678901234")
        );

        mockMvc.perform(patch("/cd/1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "CD Atualizado" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CD Atualizado"));
    }

    @Test
    void deleteCd_returns204() throws Exception {
        doNothing().when(service).deleteCD(1L);

        mockMvc.perform(delete("/cd/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCd_returns404WhenMissing() throws Exception {
        doThrow(new CdNotFoundException("Centro de distribuição não existe")).when(service).deleteCD(99L);

        mockMvc.perform(delete("/cd/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Centro de distribuição não existe"));
    }
}
