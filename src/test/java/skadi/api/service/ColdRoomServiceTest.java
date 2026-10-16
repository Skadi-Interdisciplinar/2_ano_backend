package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import skadi.api.dto.ColdRoomRequest;
import skadi.api.dto.ColdRoomResponse;
import skadi.api.exceptions.CdNotFoundException;
import skadi.api.exceptions.ColdRoomInvalidTemperatureRangeException;
import skadi.api.exceptions.ColdRoomNotFoundException;
import skadi.api.exceptions.ColdRoomRuleViolationException;
import skadi.api.exceptions.ThermometerNotFoundException;
import skadi.api.model.ColdRoom;
import skadi.api.model.DistributionCenter;
import skadi.api.model.Thermometer;
import skadi.api.repository.ColdRoomRepository;
import skadi.api.repository.DistributionCenterRepository;
import skadi.api.repository.ThermometerRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColdRoomServiceTest {

    @Mock
    private ColdRoomRepository repository;

    @Mock
    private DistributionCenterRepository distributionCenterRepository;

    @Mock
    private ThermometerRepository thermometerRepository;

    private ColdRoomService service;

    @BeforeEach
    void setUp() {
        service = new ColdRoomService(repository, distributionCenterRepository, thermometerRepository);
    }

    @Test
    void createSavesColdRoomWithExistingRelations() {
        ColdRoomRequest request = request(1L, 2L);
        when(distributionCenterRepository.findById(1L)).thenReturn(Optional.of(distributionCenter()));
        when(thermometerRepository.findById(2L)).thenReturn(Optional.of(thermometer()));
        when(repository.existsByThermometer_Id(2L)).thenReturn(false);
        when(repository.save(any(ColdRoom.class))).thenAnswer(invocation -> {
            ColdRoom coldRoom = invocation.getArgument(0);
            coldRoom.setId(3L);
            return coldRoom;
        });

        ColdRoomResponse response = service.create(request);

        ArgumentCaptor<ColdRoom> captor = ArgumentCaptor.forClass(ColdRoom.class);
        verify(repository).save(captor.capture());
        assertEquals(3L, response.id());
        assertEquals("Câmara vertical", response.model());
        assertEquals("Corredor A", response.location());
        assertEquals(1L, response.distributionCenterId());
        assertEquals(2L, response.thermometerId());
        assertEquals("Câmara vertical", captor.getValue().getModel());
    }

    @Test
    void createRejectsInvalidTemperatureRange() {
        ColdRoomRequest request = new ColdRoomRequest(
                "Câmara vertical", "Corredor A",
                new BigDecimal("5.00"), new BigDecimal("5.00"), 1L, 2L);

        assertThrows(ColdRoomInvalidTemperatureRangeException.class,
                () -> service.create(request));

        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownDistributionCenter() {
        when(distributionCenterRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CdNotFoundException.class, () -> service.create(request(1L, 2L)));
    }

    @Test
    void createRejectsUnknownThermometer() {
        when(distributionCenterRepository.findById(1L)).thenReturn(Optional.of(distributionCenter()));
        when(thermometerRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ThermometerNotFoundException.class,
                () -> service.create(request(1L, 2L)));
    }

    @Test
    void createRejectsThermometerAssignedToAnotherColdRoom() {
        when(distributionCenterRepository.findById(1L)).thenReturn(Optional.of(distributionCenter()));
        when(thermometerRepository.findById(2L)).thenReturn(Optional.of(thermometer()));
        when(repository.existsByThermometer_Id(2L)).thenReturn(true);

        assertThrows(ColdRoomRuleViolationException.class,
                () -> service.create(request(1L, 2L)));
    }

    @Test
    void findAllReturnsMappedColdRooms() {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(coldRoom()));

        List<ColdRoomResponse> response = service.findAll();

        assertEquals(1, response.size());
        assertEquals("Centro Principal", response.getFirst().distributionCenterName());
        assertEquals("DHT22", response.getFirst().thermometerModel());
    }

    @Test
    void findByIdRejectsUnknownId() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ColdRoomNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void updateChangesValuesAndKeepsAvailableThermometer() {
        ColdRoom coldRoom = coldRoom();
        when(repository.findById(3L)).thenReturn(Optional.of(coldRoom));
        when(distributionCenterRepository.findById(1L)).thenReturn(Optional.of(distributionCenter()));
        when(thermometerRepository.findById(2L)).thenReturn(Optional.of(thermometer()));
        when(repository.existsByThermometer_IdAndIdNot(2L, 3L)).thenReturn(false);
        when(repository.save(coldRoom)).thenReturn(coldRoom);

        ColdRoomResponse response = service.update(3L, request(1L, 2L));

        assertEquals("Câmara vertical", response.model());
        assertEquals(new BigDecimal("-20.00"), response.minimumTemperature());
        assertEquals(new BigDecimal("-10.00"), response.maximumTemperature());
    }

    @Test
    void updateRejectsReplacingThermometerThatHasReadings() {
        ColdRoom coldRoom = coldRoom();
        when(repository.findById(3L)).thenReturn(Optional.of(coldRoom));
        when(thermometerRepository.hasTemperatureReadings(2L)).thenReturn(true);

        assertThrows(ColdRoomRuleViolationException.class,
                () -> service.update(3L, request(1L, 4L)));

        verify(repository, never()).save(any());
    }

    @Test
    void deleteRemovesUnusedColdRoom() {
        ColdRoom coldRoom = coldRoom();
        when(repository.findById(3L)).thenReturn(Optional.of(coldRoom));
        when(repository.hasBatchStorageHistory(3L)).thenReturn(false);
        when(repository.hasAlerts(3L)).thenReturn(false);
        when(thermometerRepository.hasTemperatureReadings(2L)).thenReturn(false);

        service.delete(3L);

        verify(repository).delete(coldRoom);
        verify(repository).flush();
    }

    @Test
    void deleteRejectsColdRoomWithBatchStorageHistory() {
        when(repository.findById(3L)).thenReturn(Optional.of(coldRoom()));
        when(repository.hasBatchStorageHistory(3L)).thenReturn(true);

        assertThrows(ColdRoomRuleViolationException.class, () -> service.delete(3L));

        verify(repository, never()).delete(any());
    }

    @Test
    void deleteRejectsColdRoomWithAlerts() {
        when(repository.findById(3L)).thenReturn(Optional.of(coldRoom()));
        when(repository.hasBatchStorageHistory(3L)).thenReturn(false);
        when(repository.hasAlerts(3L)).thenReturn(true);

        assertThrows(ColdRoomRuleViolationException.class, () -> service.delete(3L));

        verify(repository, never()).delete(any());
    }

    @Test
    void deleteRejectsColdRoomWithTemperatureHistory() {
        when(repository.findById(3L)).thenReturn(Optional.of(coldRoom()));
        when(repository.hasBatchStorageHistory(3L)).thenReturn(false);
        when(repository.hasAlerts(3L)).thenReturn(false);
        when(thermometerRepository.hasTemperatureReadings(2L)).thenReturn(true);

        assertThrows(ColdRoomRuleViolationException.class, () -> service.delete(3L));

        verify(repository, never()).delete(any());
    }

    private ColdRoomRequest request(Long distributionCenterId, Long thermometerId) {
        return new ColdRoomRequest(
                "  Câmara vertical  ",
                "  Corredor A  ",
                new BigDecimal("-20.00"),
                new BigDecimal("-10.00"),
                distributionCenterId,
                thermometerId
        );
    }

    private ColdRoom coldRoom() {
        return new ColdRoom(
                3L,
                "Câmara horizontal",
                "Corredor B",
                new BigDecimal("-18.00"),
                new BigDecimal("-8.00"),
                distributionCenter(),
                thermometer()
        );
    }

    private DistributionCenter distributionCenter() {
        return new DistributionCenter(1L, "Centro Principal", "12345678000199");
    }

    private Thermometer thermometer() {
        return new Thermometer(2L, "DHT22");
    }
}
