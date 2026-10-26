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
import skadi.api.dto.ColdRoomResponse;
import skadi.api.exceptions.ColdRoomInvalidTemperatureRangeException;
import skadi.api.exceptions.ColdRoomNotFoundException;
import skadi.api.exceptions.ColdRoomRuleViolationException;
import skadi.api.service.ColdRoomService;

import java.math.BigDecimal;
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
class ColdRoomControllerTest {

    @Mock
    private ColdRoomService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ColdRoomController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201AndCreatedColdRoom() throws Exception {
        when(service.create(any(), any())).thenReturn(coldRoom());

        mockMvc.perform(post("/cold-room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/cold-rooms/3"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.model").value("Câmara vertical"))
                .andExpect(jsonPath("$.distributionCenterName").value("Centro Principal"))
                .andExpect(jsonPath("$.thermometerModel").value("DHT22"));
    }

    @Test
    void createReturns400WhenModelIsBlank() throws Exception {
        mockMvc.perform(post("/cold-room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "model": " ",
                                  "location": "Corredor A",
                                  "minimumTemperature": -20.00,
                                  "maximumTemperature": -10.00,
                                  "distributionCenterId": 1,
                                  "thermometerId": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void createReturns400WhenTemperatureRangeIsInvalid() throws Exception {
        when(service.create(any(), any())).thenThrow(new ColdRoomInvalidTemperatureRangeException());

        mockMvc.perform(post("/cold-room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "A temperatura mínima deve ser menor que a temperatura máxima"));
    }

    @Test
    void findAllReturnsColdRooms() throws Exception {
        when(service.findAll(any())).thenReturn(List.of(coldRoom()));

        mockMvc.perform(get("/cold-rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));
    }

    @Test
    void findByIdReturnsColdRoom() throws Exception {
        when(service.findById(eq(3L), any())).thenReturn(coldRoom());

        mockMvc.perform(get("/cold-rooms/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Corredor A"));
    }

    @Test
    void findByIdReturns404WhenColdRoomDoesNotExist() throws Exception {
        when(service.findById(eq(99L), any())).thenThrow(new ColdRoomNotFoundException(99L));

        mockMvc.perform(get("/cold-rooms/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    void updateReturnsUpdatedColdRoom() throws Exception {
        when(service.update(eq(3L), any(), any())).thenReturn(coldRoom());

        mockMvc.perform(put("/cold-rooms/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Câmara vertical"));
    }

    @Test
    void deleteReturns204() throws Exception {
        doNothing().when(service).delete(eq(3L), any());

        mockMvc.perform(delete("/cold-rooms/3"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns409WhenColdRoomHasRelatedData() throws Exception {
        doThrow(new ColdRoomRuleViolationException(
                "A câmara frigorífica não pode ser excluída porque possui alertas relacionados"))
                .when(service).delete(eq(3L), any());

        mockMvc.perform(delete("/cold-rooms/3"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    private ColdRoomResponse coldRoom() {
        return new ColdRoomResponse(
                3L,
                "Câmara vertical",
                "Corredor A",
                new BigDecimal("-20.00"),
                new BigDecimal("-10.00"),
                1L,
                "Centro Principal",
                2L,
                "DHT22"
        );
    }

    private String validRequest() {
        return """
                {
                  "model": "Câmara vertical",
                  "location": "Corredor A",
                  "minimumTemperature": -20.00,
                  "maximumTemperature": -10.00,
                  "distributionCenterId": 1,
                  "thermometerId": 2
                }
                """;
    }
}
