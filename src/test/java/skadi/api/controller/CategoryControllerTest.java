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
        when(service.create(any())).thenReturn(category());

        mockMvc.perform(post("/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/categorias/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Congelados"))
                .andExpect(jsonPath("$.temperaturaIdeal").value(-18.00))
                .andExpect(jsonPath("$.vidaUtilHoras").value(48.00));
    }

    @Test
    void createReturns400WhenLifeSpanIsNotPositive() throws Exception {
        mockMvc.perform(post("/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Congelados",
                                  "temperaturaIdeal": -18.00,
                                  "vidaUtilHoras": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void findAllReturnsCategories() throws Exception {
        when(service.findAll()).thenReturn(List.of(category()));

        mockMvc.perform(get("/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Congelados"));
    }

    @Test
    void findByIdReturnsCategory() throws Exception {
        when(service.findById(1L)).thenReturn(category());

        mockMvc.perform(get("/categorias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturnsUpdatedCategory() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(category());

        mockMvc.perform(put("/categorias/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Congelados"));
    }

    @Test
    void deleteReturns204() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/categorias/1"))
                .andExpect(status().isNoContent());
    }

    private CategoryResponse category() {
        return new CategoryResponse(
                1L,
                "Congelados",
                new BigDecimal("-18.00"),
                new BigDecimal("48.00")
        );
    }

    private String validRequest() {
        return """
                {
                  "nome": "Congelados",
                  "temperaturaIdeal": -18.00,
                  "vidaUtilHoras": 48.00
                }
                """;
    }
}
