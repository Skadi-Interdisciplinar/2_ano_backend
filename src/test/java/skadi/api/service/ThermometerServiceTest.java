package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import skadi.api.dto.ThermometerRequest;
import skadi.api.dto.ThermometerResponse;
import skadi.api.exceptions.ThermometerNotFoundException;
import skadi.api.exceptions.ThermometerRuleViolationException;
import skadi.api.model.Thermometer;
import skadi.api.model.User;
import skadi.api.enums.NivelAcesso;
import skadi.api.repository.ThermometerRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThermometerServiceTest {

    @Mock
    private ThermometerRepository repository;

    @Mock private DistributionCenterAccessService accessService;
    @Mock private AuditUserContext auditUserContext;

    private ThermometerService service;

    @BeforeEach
    void setUp() {
        service = new ThermometerService(repository, accessService, auditUserContext);
    }

    @Test
    void createTrimsModelAndSavesThermometer() {
        when(repository.save(any(Thermometer.class))).thenAnswer(invocation -> {
            Thermometer thermometer = invocation.getArgument(0);
            thermometer.setId(1L);
            return thermometer;
        });

        ThermometerResponse response = service.create(new ThermometerRequest("  DHT22  "), admin());

        ArgumentCaptor<Thermometer> captor = ArgumentCaptor.forClass(Thermometer.class);
        verify(repository).save(captor.capture());
        assertEquals(1L, response.id());
        assertEquals("DHT22", response.model());
        assertEquals("DHT22", captor.getValue().getModel());
    }

    @Test
    void findAllReturnsMappedThermometers() {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(
                new Thermometer(1L, "DHT22"),
                new Thermometer(2L, "DS18B20")
        ));

        List<ThermometerResponse> response = service.findAll(superAdmin());

        assertEquals(2, response.size());
        assertEquals("DHT22", response.getFirst().model());
    }

    @Test
    void findByIdReturnsThermometer() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Thermometer(1L, "DHT22")));

        ThermometerResponse response = service.findById(1L, admin());

        assertEquals(1L, response.id());
        assertEquals("DHT22", response.model());
    }

    @Test
    void findByIdRejectsUnknownId() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ThermometerNotFoundException.class, () -> service.findById(99L, admin()));
    }

    @Test
    void updateChangesAndTrimsModel() {
        Thermometer thermometer = new Thermometer(1L, "DHT11");
        when(repository.findById(1L)).thenReturn(Optional.of(thermometer));
        when(repository.save(thermometer)).thenReturn(thermometer);

        ThermometerResponse response = service.update(1L, new ThermometerRequest("  DHT22  "), admin());

        assertEquals("DHT22", response.model());
        assertEquals("DHT22", thermometer.getModel());
    }

    @Test
    void deleteRemovesUnusedThermometer() {
        Thermometer thermometer = new Thermometer(1L, "DHT22");
        when(repository.findById(1L)).thenReturn(Optional.of(thermometer));
        when(repository.isAssignedToColdRoom(1L)).thenReturn(false);
        when(repository.hasTemperatureReadings(1L)).thenReturn(false);

        service.delete(1L, admin());

        verify(repository).delete(thermometer);
        verify(repository).flush();
    }

    @Test
    void deleteRejectsThermometerAssignedToColdRoom() {
        Thermometer thermometer = new Thermometer(1L, "DHT22");
        when(repository.findById(1L)).thenReturn(Optional.of(thermometer));
        when(repository.isAssignedToColdRoom(1L)).thenReturn(true);

        assertThrows(ThermometerRuleViolationException.class, () -> service.delete(1L, admin()));

        verify(repository, never()).delete(any());
    }

    @Test
    void deleteRejectsThermometerWithTemperatureReadings() {
        Thermometer thermometer = new Thermometer(1L, "DHT22");
        when(repository.findById(1L)).thenReturn(Optional.of(thermometer));
        when(repository.isAssignedToColdRoom(1L)).thenReturn(false);
        when(repository.hasTemperatureReadings(1L)).thenReturn(true);

        assertThrows(ThermometerRuleViolationException.class, () -> service.delete(1L, admin()));

        verify(repository, never()).delete(any());
    }

    private User admin() {
        return new User(40L, "Admin", "admin", "12345678901", "admin@skadi.com",
                "senha", NivelAcesso.ADMIN, null, null);
    }

    private User superAdmin() {
        return new User(41L, "Root", "root", null, "root@skadi.com",
                "senha", NivelAcesso.SUPER_ADMIN, null, null);
    }
}
