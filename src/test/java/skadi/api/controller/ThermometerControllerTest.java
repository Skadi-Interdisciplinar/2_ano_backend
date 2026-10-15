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
import skadi.api.dto.ThermometerResponse;
import skadi.api.exceptions.ThermometerNotFoundException;
import skadi.api.exceptions.ThermometerRuleViolationException;
import skadi.api.service.ThermometerService;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ThermometerControllerTest {

    @Mock
    private ThermometerService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThermometerController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201AndCreatedThermometer() throws Exception {
        when(service.create(any())).thenReturn(thermometer());

        mockMvc.perform(post("/thermometer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/thermometers/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.model").value("DHT22"));
    }

    @Test
    void createReturns400WhenModelIsBlank() throws Exception {
        mockMvc.perform(post("/thermometer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "model": "   " }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(
                        "model: O modelo do termômetro é obrigatório"));
    }

    @Test
    void findAllReturnsThermometers() throws Exception {
        when(service.findAll()).thenReturn(List.of(thermometer()));

        mockMvc.perform(get("/thermometers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].model").value("DHT22"));
    }

    @Test
    void findByIdReturnsThermometer() throws Exception {
        when(service.findById(1L)).thenReturn(thermometer());

        mockMvc.perform(get("/thermometers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findByIdReturns404WhenThermometerDoesNotExist() throws Exception {
        when(service.findById(99L)).thenThrow(new ThermometerNotFoundException(99L));

        mockMvc.perform(get("/thermometers/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value(
                        "Termômetro não encontrado com o id 99"));
    }

    @Test
    void updateReturnsUpdatedThermometer() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(thermometer());

        mockMvc.perform(put("/thermometers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("DHT22"));
    }

    @Test
    void deleteReturns204() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/thermometers/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns409WhenThermometerIsInUse() throws Exception {
        doThrow(new ThermometerRuleViolationException(
                "O termômetro não pode ser excluído porque está associado a uma câmara frigorífica"))
                .when(service).delete(1L);

        mockMvc.perform(delete("/thermometers/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    private ThermometerResponse thermometer() {
        return new ThermometerResponse(1L, "DHT22");
    }

    private String validRequest() {
        return """
                { "model": "DHT22" }
                """;
    }
}
