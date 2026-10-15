package skadi.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import skadi.api.config.GlobalExceptionHandler;
import skadi.api.dto.StateResponse;
import skadi.api.service.StateService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StateControllerTest {

    @Mock
    private StateService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StateController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void findAllReturnsStates() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new StateResponse(1L, "AC"),
                new StateResponse(25L, "SP")
        ));

        mockMvc.perform(get("/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].abbreviation").value("AC"))
                .andExpect(jsonPath("$[1].abbreviation").value("SP"));
    }

    @Test
    void findByIdReturnsState() throws Exception {
        when(service.findById(25L)).thenReturn(new StateResponse(25L, "SP"));

        mockMvc.perform(get("/states/25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.abbreviation").value("SP"));
    }
}
