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
import skadi.api.dto.CategoryResponse;
import skadi.api.service.CategoryService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CategoryController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201AndCreatedCategory() throws Exception {
        when(service.create(any(), any())).thenReturn(category());

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/categories/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Congelados"))
                .andExpect(jsonPath("$.minimumTemperature").value(-20.00))
                .andExpect(jsonPath("$.maximumTemperature").value(-16.00))
                .andExpect(jsonPath("$.shelfLifeHours").value(48.00));
    }

    @Test
    void createReturns400WhenLifeSpanIsNotPositive() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Congelados",
                                  "minimumTemperature": -20.00,
                                  "maximumTemperature": -16.00,
                                  "shelfLifeHours": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void createReturns400WhenLifeSpanExceedsIntervalLimit() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Congelados",
                                  "minimumTemperature": -20.00,
                                  "maximumTemperature": -16.00,
                                  "shelfLifeHours": 1000.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void findAllReturnsCategories() throws Exception {
        when(service.findAll()).thenReturn(List.of(category()));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Congelados"));
    }

    @Test
    void findByIdReturnsCategory() throws Exception {
        when(service.findById(1L)).thenReturn(category());

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturnsUpdatedCategory() throws Exception {
        when(service.update(eq(1L), any(), any())).thenReturn(category());

        mockMvc.perform(put("/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Congelados"));
    }

    @Test
    void deleteReturns204() throws Exception {
        doNothing().when(service).delete(eq(1L), any());

        mockMvc.perform(delete("/categories/1"))
                .andExpect(status().isNoContent());
    }

    private CategoryResponse category() {
        return new CategoryResponse(
                1L,
                "Congelados",
                new BigDecimal("-20.00"),
                new BigDecimal("-16.00"),
                new BigDecimal("48.00")
        );
    }

    private String validRequest() {
        return """
                {
                  "name": "Congelados",
                  "minimumTemperature": -20.00,
                  "maximumTemperature": -16.00,
                  "shelfLifeHours": 48.00
                }
                """;
    }
}
