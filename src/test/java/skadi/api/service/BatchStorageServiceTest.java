package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import skadi.api.dto.BatchStorageRequest;
import skadi.api.dto.BatchStorageResponse;
import skadi.api.enums.BatchStatus;
import skadi.api.exceptions.BatchNotFoundException;
import skadi.api.exceptions.BatchStorageNotFoundException;
import skadi.api.exceptions.BatchStorageRuleViolationException;
import skadi.api.exceptions.ColdRoomNotFoundException;
import skadi.api.model.Batch;
import skadi.api.model.BatchStorage;
import skadi.api.model.Category;
import skadi.api.model.ColdRoom;
import skadi.api.model.DistributionCenter;
import skadi.api.model.Thermometer;
import skadi.api.model.User;
import skadi.api.enums.NivelAcesso;
import skadi.api.repository.BatchRepository;
import skadi.api.repository.BatchStorageRepository;
import skadi.api.repository.ColdRoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchStorageServiceTest {

    @Mock
    private BatchStorageRepository storageRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private ColdRoomRepository coldRoomRepository;

    @Mock private DistributionCenterAccessService accessService;
    @Mock private AuditUserContext auditUserContext;

    private BatchStorageService service;

    @BeforeEach
    void setUp() {
        service = new BatchStorageService(storageRepository, batchRepository, coldRoomRepository,
                accessService, auditUserContext);
    }

    @Test
    void enterCreatesActiveStorage() {
        when(batchRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(batch()));
        when(storageRepository.findByBatch_IdAndExitDateIsNull(1L)).thenReturn(Optional.empty());
        when(coldRoomRepository.findById(3L)).thenReturn(Optional.of(coldRoom(3L)));
        when(storageRepository.saveAndFlush(any(BatchStorage.class))).thenAnswer(invocation -> {
            BatchStorage storage = invocation.getArgument(0);
            storage.setId(10L);
            return storage;
        });

        BatchStorageResponse response = service.enter(1L, new BatchStorageRequest(3L), operator());

        ArgumentCaptor<BatchStorage> captor = ArgumentCaptor.forClass(BatchStorage.class);
        verify(storageRepository).saveAndFlush(captor.capture());
        assertEquals(10L, response.id());
        assertEquals(1L, response.batchId());
        assertEquals(3L, response.coldRoomId());
        assertTrue(response.active());
        assertNull(captor.getValue().getExitDate());
    }

    @Test
    void enterRejectsBatchThatIsAlreadyStored() {
        when(batchRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(batch()));
        when(storageRepository.findByBatch_IdAndExitDateIsNull(1L))
                .thenReturn(Optional.of(activeStorage(10L, coldRoom(3L))));

        assertThrows(BatchStorageRuleViolationException.class,
                () -> service.enter(1L, new BatchStorageRequest(4L), operator()));

        verify(coldRoomRepository, never()).findById(any());
        verify(storageRepository, never()).saveAndFlush(any());
    }

    @Test
    void enterRejectsUnknownBatch() {
        when(batchRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(BatchNotFoundException.class,
                () -> service.enter(99L, new BatchStorageRequest(3L), operator()));
    }

    @Test
    void enterRejectsUnknownColdRoom() {
        when(batchRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(batch()));
        when(storageRepository.findByBatch_IdAndExitDateIsNull(1L)).thenReturn(Optional.empty());
        when(coldRoomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ColdRoomNotFoundException.class,
                () -> service.enter(1L, new BatchStorageRequest(99L), operator()));
    }

    @Test
    void enterTranslatesDatabaseTemperatureValidation() {
        when(batchRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(batch()));
        when(storageRepository.findByBatch_IdAndExitDateIsNull(1L)).thenReturn(Optional.empty());
        when(coldRoomRepository.findById(3L)).thenReturn(Optional.of(coldRoom(3L)));
        when(storageRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("trigger rejected storage"));

        BatchStorageRuleViolationException exception = assertThrows(
                BatchStorageRuleViolationException.class,
                () -> service.enter(1L, new BatchStorageRequest(3L), operator())
        );

        assertEquals(
                "A faixa de temperatura da categoria do lote é incompatível com a câmara frigorífica",
                exception.getMessage()
        );
    }

    @Test
    void findHistoryReturnsMostRecentStorages() {
        BatchStorage closed = activeStorage(9L, coldRoom(3L));
        closed.setExitDate(LocalDateTime.now());
        when(batchRepository.existsById(1L)).thenReturn(true);
        when(storageRepository.findFirstByBatch_IdOrderByEntryDateAsc(1L))
                .thenReturn(Optional.of(closed));
        when(storageRepository.findByBatch_IdOrderByEntryDateDesc(1L))
                .thenReturn(List.of(activeStorage(10L, coldRoom(4L)), closed));

        List<BatchStorageResponse> response = service.findHistory(1L, operator());

        assertEquals(2, response.size());
        assertTrue(response.getFirst().active());
        assertFalse(response.getLast().active());
    }

    @Test
    void findHistoryRejectsUnknownBatch() {
        when(batchRepository.existsById(99L)).thenReturn(false);

        assertThrows(BatchNotFoundException.class, () -> service.findHistory(99L, operator()));
    }

    @Test
    void moveClosesCurrentStorageAndCreatesDestinationStorage() {
        BatchStorage current = activeStorage(10L, coldRoom(3L));
        when(batchRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(batch()));
        when(storageRepository.findByIdAndBatch_Id(10L, 1L)).thenReturn(Optional.of(current));
        when(coldRoomRepository.findById(4L)).thenReturn(Optional.of(coldRoom(4L)));
        when(storageRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            BatchStorage storage = invocation.getArgument(0);
            if (storage.getId() == null) {
                storage.setId(11L);
            }
            return storage;
        });

        BatchStorageResponse response = service.move(
                1L, 10L, new BatchStorageRequest(4L), operator());

        assertEquals(11L, response.id());
        assertEquals(4L, response.coldRoomId());
        assertTrue(response.active());
        assertEquals(current.getExitDate(), response.entryDate());
        verify(storageRepository, org.mockito.Mockito.times(2)).saveAndFlush(any());
    }

    @Test
    void moveRejectsSameColdRoom() {
        when(batchRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(batch()));
        when(storageRepository.findByIdAndBatch_Id(10L, 1L))
                .thenReturn(Optional.of(activeStorage(10L, coldRoom(3L))));

        assertThrows(BatchStorageRuleViolationException.class,
                () -> service.move(1L, 10L, new BatchStorageRequest(3L), operator()));

        verify(storageRepository, never()).saveAndFlush(any());
    }

    @Test
    void moveRejectsClosedStorage() {
        BatchStorage storage = activeStorage(10L, coldRoom(3L));
        storage.setExitDate(LocalDateTime.now());
        when(batchRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(batch()));
        when(storageRepository.findByIdAndBatch_Id(10L, 1L)).thenReturn(Optional.of(storage));

        assertThrows(BatchStorageRuleViolationException.class,
                () -> service.move(1L, 10L, new BatchStorageRequest(4L), operator()));
    }

    @Test
    void moveRejectsDestinationFromAnotherDistributionCenter() {
        BatchStorage current = activeStorage(10L, coldRoom(3L, 5L));
        when(batchRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(batch()));
        when(storageRepository.findByIdAndBatch_Id(10L, 1L)).thenReturn(Optional.of(current));
        when(coldRoomRepository.findById(4L)).thenReturn(Optional.of(coldRoom(4L, 6L)));

        BatchStorageRuleViolationException exception = assertThrows(
                BatchStorageRuleViolationException.class,
                () -> service.move(1L, 10L, new BatchStorageRequest(4L), operator()));

        assertEquals("O lote não pode ser movimentado entre centros de distribuição diferentes",
                exception.getMessage());
        verify(storageRepository, never()).saveAndFlush(any());
    }

    @Test
    void exitClosesActiveStorage() {
        BatchStorage storage = activeStorage(10L, coldRoom(3L));
        when(batchRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(batch()));
        when(storageRepository.findByIdAndBatch_Id(10L, 1L)).thenReturn(Optional.of(storage));
        when(storageRepository.saveAndFlush(storage)).thenReturn(storage);

        BatchStorageResponse response = service.exit(1L, 10L, operator());

        assertFalse(response.active());
        assertTrue(response.exitDate().isAfter(response.entryDate()));
    }

    @Test
    void exitRejectsStorageFromAnotherBatch() {
        when(batchRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(batch()));
        when(storageRepository.findByIdAndBatch_Id(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(BatchStorageNotFoundException.class,
                () -> service.exit(1L, 99L, operator()));
    }

    private BatchStorage activeStorage(Long id, ColdRoom coldRoom) {
        return new BatchStorage(
                id,
                batch(),
                coldRoom,
                LocalDateTime.now().minusHours(1),
                null
        );
    }

    private Batch batch() {
        return new Batch(
                1L,
                "LOTE-2026-0015",
                new Category(2L, "Carne bovina", new BigDecimal("-20.00"),
                        new BigDecimal("-10.00"), Duration.ofHours(72)),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 30),
                BatchStatus.ACTIVE
        );
    }

    private ColdRoom coldRoom(Long id) {
        return coldRoom(id, 5L);
    }

    private ColdRoom coldRoom(Long id, Long distributionCenterId) {
        return new ColdRoom(
                id,
                "Câmara " + id,
                "Corredor A",
                new BigDecimal("-20.00"),
                new BigDecimal("-10.00"),
                new DistributionCenter(5L, "Centro Principal", "12345678000199", null),
                new Thermometer(id + 10, "DHT22")
        );
    }

    private User operator() {
        return new User(70L, "Operador", "operador", "12345678901", "op@skadi.com",
                "senha", NivelAcesso.OPERADOR,
                new DistributionCenter(5L, "Centro Principal", "12345678000199"), 80);
    }
}
