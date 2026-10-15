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
import skadi.api.dto.BatchResponse;
import skadi.api.enums.BatchStatus;
import skadi.api.service.BatchService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BatchControllerTest {

    @Mock
    private BatchService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BatchController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201AndCreatedBatch() throws Exception {
        when(service.create(any())).thenReturn(batch());

        mockMvc.perform(post("/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/batches/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.batchCode").value("BATCH-001"))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Congelados"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createReturns400WhenBatchCodeIsBlank() throws Exception {
        mockMvc.perform(post("/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "batchCode": " ",
                                  "categoryId": 1,
                                  "manufacturingDate": "2026-10-01",
                                  "expirationDate": "2026-10-31"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void findAllReturnsBatches() throws Exception {
        when(service.findAll()).thenReturn(List.of(batch()));

        mockMvc.perform(get("/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchCode").value("BATCH-001"));
    }

    @Test
    void findByIdReturnsBatch() throws Exception {
        when(service.findById(1L)).thenReturn(batch());

        mockMvc.perform(get("/batches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturnsUpdatedBatch() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(batch());

        mockMvc.perform(put("/batches/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchCode").value("BATCH-001"));
    }

    @Test
    void updateStatusReturnsUpdatedBatch() throws Exception {
        BatchResponse blockedBatch = new BatchResponse(
                1L,
                "BATCH-001",
                1L,
                "Congelados",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                BatchStatus.BLOCKED
        );
        when(service.updateStatus(1L, BatchStatus.BLOCKED)).thenReturn(blockedBatch);

        mockMvc.perform(patch("/batches/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "BLOCKED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void updateStatusReturns400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(patch("/batches/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "UNKNOWN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void deleteReturns204() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/batches/1"))
                .andExpect(status().isNoContent());
    }

    private BatchResponse batch() {
        return new BatchResponse(
                1L,
                "BATCH-001",
                1L,
                "Congelados",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                BatchStatus.ACTIVE
        );
    }

    private String validRequest() {
        return """
                {
                  "batchCode": "BATCH-001",
                  "categoryId": 1,
                  "manufacturingDate": "2026-10-01",
                  "expirationDate": "2026-10-31"
                }
                """;
    }
}
