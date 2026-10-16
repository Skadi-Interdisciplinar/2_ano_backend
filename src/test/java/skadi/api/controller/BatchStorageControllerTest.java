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
import skadi.api.dto.BatchStorageResponse;
import skadi.api.exceptions.BatchStorageNotFoundException;
import skadi.api.exceptions.BatchStorageRuleViolationException;
import skadi.api.service.BatchStorageService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BatchStorageControllerTest {

    @Mock
    private BatchStorageService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BatchStorageController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void enterReturns201AndActiveStorage() throws Exception {
        when(service.enter(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(activeStorage());

        mockMvc.perform(post("/batches/1/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"coldRoomId\": 3 }"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/batches/1/storages/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.coldRoomId").value(3))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void enterReturns400WithoutColdRoom() throws Exception {
        mockMvc.perform(post("/batches/1/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void enterReturns409WhenBatchIsAlreadyStored() throws Exception {
        when(service.enter(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenThrow(new BatchStorageRuleViolationException("O lote já está armazenado"));

        mockMvc.perform(post("/batches/1/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"coldRoomId\": 3 }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O lote já está armazenado"));
    }

    @Test
    void findHistoryReturnsStorages() throws Exception {
        when(service.findHistory(1L)).thenReturn(List.of(activeStorage()));

        mockMvc.perform(get("/batches/1/storages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchCode").value("LOTE-2026-0015"));
    }

    @Test
    void moveReturnsDestinationStorage() throws Exception {
        BatchStorageResponse moved = response(11L, 4L, null);
        when(service.move(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L), any())).thenReturn(moved);

        mockMvc.perform(put("/batches/1/storages/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"coldRoomId\": 4 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.coldRoomId").value(4))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void exitReturnsClosedStorage() throws Exception {
        LocalDateTime exitDate = LocalDateTime.of(2026, 10, 16, 10, 0);
        when(service.exit(1L, 10L)).thenReturn(response(10L, 3L, exitDate));

        mockMvc.perform(patch("/batches/1/storages/10/exit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.exitDate").exists());
    }

    @Test
    void exitReturns404ForUnknownStorage() throws Exception {
        when(service.exit(1L, 99L)).thenThrow(new BatchStorageNotFoundException(99L, 1L));

        mockMvc.perform(patch("/batches/1/storages/99/exit"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    private BatchStorageResponse activeStorage() {
        return response(10L, 3L, null);
    }

    private BatchStorageResponse response(Long id, Long coldRoomId, LocalDateTime exitDate) {
        return new BatchStorageResponse(
                id,
                1L,
                "LOTE-2026-0015",
                coldRoomId,
                "Câmara vertical",
                "Corredor A",
                5L,
                "Centro Principal",
                LocalDateTime.of(2026, 10, 16, 9, 0),
                exitDate,
                exitDate == null
        );
    }
}
