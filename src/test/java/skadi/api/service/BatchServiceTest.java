package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.BatchRequest;
import skadi.api.dto.BatchResponse;
import skadi.api.enums.BatchStatus;
import skadi.api.exceptions.BatchAlreadyExistsException;
import skadi.api.exceptions.BatchInvalidDatesException;
import skadi.api.exceptions.BatchNotFoundException;
import skadi.api.exceptions.BatchRuleViolationException;
import skadi.api.exceptions.CategoryNotFoundException;
import skadi.api.model.Batch;
import skadi.api.model.Category;
import skadi.api.model.BatchStorage;
import skadi.api.model.ColdRoom;
import skadi.api.model.DistributionCenter;
import skadi.api.model.Thermometer;
import skadi.api.model.User;
import skadi.api.enums.NivelAcesso;
import skadi.api.repository.BatchRepository;
import skadi.api.repository.BatchStorageRepository;
import skadi.api.repository.CategoryRepository;
import skadi.api.repository.ColdRoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock private BatchStorageRepository storageRepository;
    @Mock private ColdRoomRepository coldRoomRepository;
    @Mock private DistributionCenterAccessService accessService;
    @Mock private AuditUserContext auditUserContext;

    private BatchService service;

    @BeforeEach
    void setUp() {
        service = new BatchService(batchRepository, categoryRepository, storageRepository,
                coldRoomRepository, accessService, auditUserContext);
    }

    @Test
    void createSavesActiveBatchWhenDataIsValid() {
        BatchRequest request = request("  BATCH-001  ", 1L);
        Category category = category(1L, "Congelados");
        when(batchRepository.existsByBatchCodeIgnoreCase("BATCH-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(coldRoomRepository.findById(3L)).thenReturn(Optional.of(coldRoom()));
        when(batchRepository.save(any(Batch.class))).thenAnswer(invocation -> {
            Batch batch = invocation.getArgument(0);
            batch.setId(1L);
            return batch;
        });

        when(storageRepository.saveAndFlush(any(BatchStorage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        BatchResponse response = service.create(request, operator());

        ArgumentCaptor<Batch> captor = ArgumentCaptor.forClass(Batch.class);
        verify(batchRepository).save(captor.capture());
        assertEquals(1L, response.id());
        assertEquals("BATCH-001", response.batchCode());
        assertEquals(1L, response.categoryId());
        assertEquals(BatchStatus.ACTIVE, response.status());
        assertEquals("BATCH-001", captor.getValue().getBatchCode());
    }

    @Test
    void createRejectsDuplicatedCode() {
        BatchRequest request = request("BATCH-001", 1L);
        when(batchRepository.existsByBatchCodeIgnoreCase("BATCH-001")).thenReturn(true);

        assertThrows(BatchAlreadyExistsException.class, () -> service.create(request, operator()));
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void createRejectsExpirationBeforeManufacturing() {
        BatchRequest request = new BatchRequest(
                "BATCH-001",
                1L,
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 9),
                3L
        );

        assertThrows(BatchInvalidDatesException.class, () -> service.create(request, operator()));
        verify(batchRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownCategory() {
        BatchRequest request = request("BATCH-001", 99L);
        when(batchRepository.existsByBatchCodeIgnoreCase("BATCH-001")).thenReturn(false);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> service.create(request, operator()));
    }

    @Test
    void findByIdReturnsBatch() {
        Batch batch = batch();
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(storageRepository.findFirstByBatch_IdOrderByEntryDateAsc(1L))
                .thenReturn(Optional.of(storage()));

        BatchResponse response = service.findById(1L, operator());

        assertEquals(batch.getId(), response.id());
        assertEquals(batch.getCategory().getName(), response.categoryName());
    }

    @Test
    void findByIdRejectsUnknownId() {
        when(batchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BatchNotFoundException.class, () -> service.findById(99L, operator()));
    }

    @Test
    void updateChangesBatchValues() {
        Batch batch = batch();
        Category newCategory = category(2L, "Resfriados");
        BatchRequest request = request("BATCH-UPDATED", 2L);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(storageRepository.findFirstByBatch_IdOrderByEntryDateAsc(1L))
                .thenReturn(Optional.of(storage()));
        when(batchRepository.existsByBatchCodeIgnoreCaseAndIdNot("BATCH-UPDATED", 1L)).thenReturn(false);
        when(batchRepository.hasActiveStorage(1L)).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(batchRepository.save(batch)).thenReturn(batch);

        BatchResponse response = service.update(1L, request, operator());

        assertEquals("BATCH-UPDATED", response.batchCode());
        assertEquals(2L, response.categoryId());
        assertEquals("Resfriados", response.categoryName());
    }

    @Test
    void updateRejectsCategoryChangeWhileBatchIsStored() {
        Batch batch = batch();
        BatchRequest request = request("BATCH-001", 2L);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(storageRepository.findFirstByBatch_IdOrderByEntryDateAsc(1L))
                .thenReturn(Optional.of(storage()));
        when(batchRepository.existsByBatchCodeIgnoreCaseAndIdNot("BATCH-001", 1L)).thenReturn(false);
        when(batchRepository.hasActiveStorage(1L)).thenReturn(true);

        assertThrows(BatchRuleViolationException.class, () -> service.update(1L, request, operator()));
        verify(categoryRepository, never()).findById(2L);
    }

    @Test
    void updateStatusChangesStatus() {
        Batch batch = batch();
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(storageRepository.findFirstByBatch_IdOrderByEntryDateAsc(1L))
                .thenReturn(Optional.of(storage()));
        when(batchRepository.save(batch)).thenReturn(batch);

        BatchResponse response = service.updateStatus(1L, BatchStatus.BLOCKED, operator());

        assertEquals(BatchStatus.BLOCKED, response.status());
    }

    @Test
    void deleteRejectsBatchWithStorageHistory() {
        Batch batch = batch();
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(storageRepository.findFirstByBatch_IdOrderByEntryDateAsc(1L))
                .thenReturn(Optional.of(storage()));
        when(batchRepository.hasStorageHistory(1L)).thenReturn(true);

        assertThrows(BatchRuleViolationException.class, () -> service.delete(1L, operator()));
        verify(batchRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesBatchWithoutStorageHistory() {
        Batch batch = batch();
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(storageRepository.findFirstByBatch_IdOrderByEntryDateAsc(1L))
                .thenReturn(Optional.of(storage()));
        when(batchRepository.hasStorageHistory(1L)).thenReturn(false);

        service.delete(1L, operator());

        verify(batchRepository).delete(batch);
        verify(batchRepository).flush();
    }

    private BatchRequest request(String batchCode, Long categoryId) {
        return new BatchRequest(
                batchCode,
                categoryId,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                3L
        );
    }

    private Batch batch() {
        return new Batch(
                1L,
                "BATCH-001",
                category(1L, "Congelados"),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                BatchStatus.ACTIVE
        );
    }

    private Category category(Long id, String name) {
        return new Category(
                id,
                name,
                new BigDecimal("-20.00"),
                new BigDecimal("-10.00"),
                Duration.ofHours(720)
        );
    }

    private BatchStorage storage() {
        return new BatchStorage(10L, batch(), coldRoom(), java.time.LocalDateTime.now(), null);
    }

    private ColdRoom coldRoom() {
        return new ColdRoom(3L, "Câmara", "Corredor A",
                new BigDecimal("-20.00"), new BigDecimal("-10.00"),
                new DistributionCenter(5L, "CD", "12345678000199", null),
                new Thermometer(7L, "DHT22"));
    }

    private User operator() {
        return new User(50L, "Operador", "operador", "12345678901", "op@skadi.com",
                "senha", NivelAcesso.OPERADOR,
                new DistributionCenter(5L, "CD", "12345678000199", null), null, 60);
    }
}
