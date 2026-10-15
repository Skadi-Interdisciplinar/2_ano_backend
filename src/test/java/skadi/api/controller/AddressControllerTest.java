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
import skadi.api.dto.AddressResponse;
import skadi.api.service.AddressService;

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
class AddressControllerTest {

    @Mock
    private AddressService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AddressController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201AndLocation() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(address());

        mockMvc.perform(post("/distribution-centers/1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/addresses/10"))
                .andExpect(jsonPath("$.zipCode").value("01001000"))
                .andExpect(jsonPath("$.stateAbbreviation").value("SP"));
    }

    @Test
    void createReturns400ForInvalidZipCode() throws Exception {
        mockMvc.perform(post("/distribution-centers/1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("01001000", "01001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void createReturns400ForNegativeNumber() throws Exception {
        mockMvc.perform(post("/distribution-centers/1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("\"number\": 100", "\"number\": -1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAllByDistributionCenterReturnsAddresses() throws Exception {
        when(service.findAllByDistributionCenter(1L)).thenReturn(List.of(address()));

        mockMvc.perform(get("/distribution-centers/1/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].distributionCenterId").value(1));
    }

    @Test
    void findByIdReturnsAddress() throws Exception {
        when(service.findById(10L)).thenReturn(address());

        mockMvc.perform(get("/addresses/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Rua das Flores"));
    }

    @Test
    void updateReturnsUpdatedAddress() throws Exception {
        when(service.update(eq(10L), any())).thenReturn(address());

        mockMvc.perform(put("/addresses/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void deleteReturns204() throws Exception {
        doNothing().when(service).delete(10L);

        mockMvc.perform(delete("/addresses/10"))
                .andExpect(status().isNoContent());
    }

    private AddressResponse address() {
        return new AddressResponse(
                10L, "01001000", "Rua das Flores", 100, "São Paulo",
                "Centro", null, 25L, "SP", 1L, "CD São Paulo"
        );
    }

    private String validRequest() {
        return """
                {
                  "zipCode": "01001000",
                  "street": "Rua das Flores",
                  "number": 100,
                  "city": "São Paulo",
                  "neighborhood": "Centro",
                  "complement": null,
                  "stateId": 25
                }
                """;
    }
}
