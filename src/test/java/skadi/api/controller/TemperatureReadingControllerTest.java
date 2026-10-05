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
import skadi.api.dto.TemperatureReadingResponse;
import skadi.api.dto.TemperatureReadingView;
import skadi.api.service.TemperatureReadingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TemperatureReadingControllerTest {

    @Mock
    private TemperatureReadingService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TemperatureReadingController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void publish_returns202WhenReadingIsAccepted() throws Exception {
        when(service.publish(any())).thenReturn(new TemperatureReadingResponse(
                "evt-123",
                "1740000000000-0",
                "received",
                OffsetDateTime.parse("2026-10-03T18:00:00-03:00")
        ));

        mockMvc.perform(post("/reading")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "thermometerId": 1,
                                  "temperature": -12.50,
                                  "measuredAt": "2026-10-03T18:00:00-03:00"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("evt-123"))
                .andExpect(jsonPath("$.redisMessageId").value("1740000000000-0"))
                .andExpect(jsonPath("$.status").value("received"));
    }

    @Test
    void publish_returns400WhenRequiredFieldIsMissing() throws Exception {
        mockMvc.perform(post("/reading")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "temperature": -12.50,
                                  "measuredAt": "2026-10-03T18:00:00-03:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void findAll_returnsReadingsFromPostgres() throws Exception {
        when(service.findAll()).thenReturn(List.of(reading()));

        mockMvc.perform(get("/readings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].readingId").value(10))
                .andExpect(jsonPath("$[0].thermometerId").value(7))
                .andExpect(jsonPath("$[0].coldRoomId").value(3))
                .andExpect(jsonPath("$[0].temperature").value(-12.50))
                .andExpect(jsonPath("$[0].eventId").value("evt-123"));
    }

    @Test
    void findByColdRoom_returnsReadingsForTheColdRoom() throws Exception {
        when(service.findHistoryByColdRoom(3L, null, null)).thenReturn(List.of(reading()));

        mockMvc.perform(get("/readings/cold-rooms/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].coldRoomId").value(3))
                .andExpect(jsonPath("$[0].thermometerId").value(7));
    }

    @Test
    void findCurrentByColdRoom_returns404WhenThereIsNoReading() throws Exception {
        when(service.findCurrentByColdRoom(3L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/readings/cold-rooms/3/current-temperature"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findHistoryByColdRoom_acceptsAnOptionalPeriod() throws Exception {
        when(service.findHistoryByColdRoom(
                org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 10, 1, 0, 0)),
                org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 10, 4, 23, 59))
        )).thenReturn(List.of(reading()));

        mockMvc.perform(get("/readings/cold-rooms/3/temperature-history")
                        .param("from", "2026-10-01T00:00:00-03:00")
                        .param("to", "2026-10-04T23:59:00-03:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].readingId").value(10));
    }

    @Test
    void findCurrentByColdRoom_returnsTheMostRecentReading() throws Exception {
        when(service.findCurrentByColdRoom(3L)).thenReturn(java.util.Optional.of(reading()));

        mockMvc.perform(get("/readings/cold-rooms/3/current-temperature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readingId").value(10))
                .andExpect(jsonPath("$.temperature").value(-12.50));
    }

    private TemperatureReadingView reading() {
        return new TemperatureReadingView(
                10L,
                7L,
                3L,
                5L,
                new BigDecimal("-12.50"),
                LocalDateTime.of(2026, 10, 4, 10, 0),
                "evt-123"
        );
    }
}
