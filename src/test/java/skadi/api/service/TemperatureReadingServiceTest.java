package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import skadi.api.repository.TemperatureReadingRepository;
import skadi.api.repository.ColdRoomRepository;
import skadi.api.repository.ThermometerRepository;
import org.springframework.test.util.ReflectionTestUtils;
import skadi.api.dto.TemperatureReadingRequest;
import skadi.api.dto.TemperatureReadingResponse;
import skadi.api.dto.TemperatureReadingView;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.ColdRoomNotFoundException;
import skadi.api.exceptions.TemperatureReadingNotFoundException;
import skadi.api.exceptions.ResourceOutsideScopeException;
import skadi.api.exceptions.ThermometerNotFoundException;
import skadi.api.model.User;
import skadi.api.model.DistributionCenter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class TemperatureReadingServiceTest {

    private static final LocalDateTime MEASURED_AT = LocalDateTime.of(2026, 10, 4, 10, 0);
    private static final Integer CD_ID = 1;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, String, String> streamOperations;

    @Mock
    private TemperatureReadingRepository repository;

    @Mock
    private ColdRoomRepository coldRoomRepository;

    @Mock
    private DistributionCenterAccessService accessService;

    @Mock
    private ThermometerRepository thermometerRepository;

    private TemperatureReadingService service;

    @BeforeEach
    void setUp() {
        service = new TemperatureReadingService(
                redisTemplate, repository, coldRoomRepository, accessService, thermometerRepository);
        lenient().when(thermometerRepository.findDistributionCenterId(7L)).thenReturn(Optional.of(1L));
        ReflectionTestUtils.setField(service, "temperatureStream", "skadi:leituras:temperatura");
        ReflectionTestUtils.setField(service, "temperatureRetentionHours", 72L);
    }

    @Test
    void publish_sendsWorkerCompatibleFieldsToRedis() {
        givenRedisStreamOperations();
        when(streamOperations.add(any(MapRecord.class)))
                .thenReturn(RecordId.of("1740000000000-0"));

        TemperatureReadingRequest request = new TemperatureReadingRequest(
                7L,
                new BigDecimal("-12.50"),
                OffsetDateTime.parse("2026-10-03T18:00:00-03:00")
        );

        TemperatureReadingResponse response = service.publish(request, adminUser());

        ArgumentCaptor<MapRecord> captor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(captor.capture());

        @SuppressWarnings("unchecked")
        MapRecord<String, String, String> record = captor.getValue();
        assertEquals("skadi:leituras:temperatura", record.getStream());
        assertEquals(response.eventId(), record.getValue().get("id_evento_redis"));
        assertEquals("7", record.getValue().get("cod_termometro"));
        assertEquals("-12.50", record.getValue().get("temperatura"));
        assertEquals("2026-10-03T18:00-03:00", record.getValue().get("data_hora"));
        assertEquals("1740000000000-0", response.redisMessageId());
        assertEquals("received", response.status());
        verify(streamOperations).trim(eq("skadi:leituras:temperatura"), any(RedisStreamCommands.XTrimOptions.class));
    }

    @Test
    void publish_generatesTheSameEventIdForTheSameReading() {
        givenRedisStreamOperations();
        when(streamOperations.add(any(MapRecord.class)))
                .thenReturn(RecordId.of("1740000000000-0"), RecordId.of("1740000000001-0"));

        TemperatureReadingRequest request = new TemperatureReadingRequest(
                7L,
                new BigDecimal("-12.50"),
                OffsetDateTime.parse("2026-10-03T18:00:00-03:00")
        );

        TemperatureReadingResponse first = service.publish(request, adminUser());
        TemperatureReadingResponse second = service.publish(request, adminUser());

        assertEquals(first.eventId(), second.eventId());
    }

    @Test
    void publish_trimsRedisStreamUsingConfiguredRetention() {
        givenRedisStreamOperations();
        when(streamOperations.add(any(MapRecord.class)))
                .thenReturn(RecordId.of("1740000000000-0"));

        service.publish(request(), adminUser());

        verify(streamOperations).trim(eq("skadi:leituras:temperatura"), any(RedisStreamCommands.XTrimOptions.class));
    }

    @Test
    void publish_doesNotTrimWhenRedisPublishFails() {
        givenRedisStreamOperations();
        when(streamOperations.add(any(MapRecord.class)))
                .thenThrow(new IllegalStateException("Redis indisponível"));

        assertThrows(IllegalStateException.class, () -> service.publish(request(), adminUser()));

        verify(streamOperations, never())
                .trim(any(String.class), any(RedisStreamCommands.XTrimOptions.class));
    }

    @Test
    void publishRejectsThermometerOutsideAdminsDistributionCenters() {
        doThrow(new ResourceOutsideScopeException())
                .when(accessService).requireAdminOwnership(adminUser(), 1L);

        assertThrows(ResourceOutsideScopeException.class,
                () -> service.publish(request(), adminUser()));

        org.mockito.Mockito.verifyNoInteractions(redisTemplate);
    }

    @Test
    void publishRejectsThermometerThatIsNotAssignedToAColdRoom() {
        when(thermometerRepository.findDistributionCenterId(7L)).thenReturn(Optional.empty());

        assertThrows(ThermometerNotFoundException.class,
                () -> service.publish(request(), adminUser()));

        org.mockito.Mockito.verifyNoInteractions(redisTemplate);
    }

    @Test
    void findAll_mapsRepositoryRowsToViews() {
        when(repository.findAllWithColdRoomByCd(CD_ID, false))
                .thenReturn(List.<Object[]>of(row(10, 7, 3, 5)));

        List<TemperatureReadingView> result = service.findAll(CD_ID, NivelAcesso.OPERADOR);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).readingId());
        assertEquals(7L, result.get(0).thermometerId());
        assertEquals(3L, result.get(0).coldRoomId());
        assertEquals(5L, result.get(0).alertId());
        assertEquals(new BigDecimal("-12.50"), result.get(0).temperature());
        assertEquals(MEASURED_AT, result.get(0).measuredAt());
        verify(repository).findAllWithColdRoomByCd(CD_ID, false);
    }

    @Test
    void findById_returnsMappedReadingWithinUsersCd() {
        when(repository.findByReadingIdAndCd(10, CD_ID, false))
                .thenReturn(List.<Object[]>of(row(10, 7, 3, 5)));

        TemperatureReadingView result = service.findById(10, CD_ID, NivelAcesso.OPERADOR);

        assertEquals(10L, result.readingId());
        assertEquals(3L, result.coldRoomId());
        assertEquals(5L, result.alertId());
        verify(repository).findByReadingIdAndCd(10, CD_ID, false);
    }

    @Test
    void findById_throwsNotFoundWhenReadingDoesNotExistOrBelongsToAnotherCd() {
        when(repository.findByReadingIdAndCd(10, CD_ID, false)).thenReturn(List.of());

        assertThrows(TemperatureReadingNotFoundException.class,
                () -> service.findById(10, CD_ID, NivelAcesso.OPERADOR));
    }

    @Test
    void findCurrentByColdRoom_returnsMappedMostRecentReading() {
        when(repository.existsColdRoomByIdAndCd(3L, CD_ID, false)).thenReturn(true);
        when(repository.findCurrentByColdRoomIdAndCd(3L, CD_ID, false))
                .thenReturn(List.<Object[]>of(row(10, 7, 3, null)));

        Optional<TemperatureReadingView> result = service.findCurrentByColdRoom(
                3L, CD_ID, NivelAcesso.OPERADOR);

        assertEquals(10L, result.orElseThrow().readingId());
        assertEquals(3L, result.orElseThrow().coldRoomId());
        assertEquals(null, result.orElseThrow().alertId());
        assertEquals(MEASURED_AT, result.orElseThrow().measuredAt());
        verify(repository).findCurrentByColdRoomIdAndCd(3L, CD_ID, false);
    }

    @Test
    void findCurrentByColdRoom_returnsEmptyWhenRepositoryHasNoReading() {
        when(repository.existsColdRoomByIdAndCd(3L, CD_ID, false)).thenReturn(true);
        when(repository.findCurrentByColdRoomIdAndCd(3L, CD_ID, false)).thenReturn(List.of());

        assertEquals(Optional.empty(), service.findCurrentByColdRoom(3L, CD_ID, NivelAcesso.OPERADOR));
    }

    @Test
    void findHistoryByColdRoom_forwardsPeriodToRepositoryAndMapsRows() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 4, 23, 59);
        when(repository.existsColdRoomByIdAndCd(3L, CD_ID, false)).thenReturn(true);
        when(repository.findHistoryByColdRoomIdAndCd(3L, CD_ID, false, from, to))
                .thenReturn(List.<Object[]>of(row(10, 7, 3, 5)));

        List<TemperatureReadingView> result = service.findHistoryByColdRoom(
                3L, CD_ID, NivelAcesso.OPERADOR, from, to);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).readingId());
        assertEquals(MEASURED_AT, result.get(0).measuredAt());
        verify(repository).findHistoryByColdRoomIdAndCd(3L, CD_ID, false, from, to);
    }

    @Test
    void findAll_mapsTimestampRepositoryValue() {
        Object[] row = row(10, 7, 3, 5);
        row[5] = Timestamp.valueOf(MEASURED_AT);
        when(repository.findAllWithColdRoomByCd(CD_ID, false)).thenReturn(List.<Object[]>of(row));

        assertEquals(MEASURED_AT, service.findAll(CD_ID, NivelAcesso.OPERADOR).get(0).measuredAt());
    }

    @Test
    void getMethods_rejectUsersWithoutCd() {
        assertThrows(AccessDeniedException.class, () -> service.findAll(null, NivelAcesso.OPERADOR));
        assertThrows(AccessDeniedException.class,
                () -> service.findCurrentByColdRoom(3L, null, NivelAcesso.OPERADOR));
        assertThrows(AccessDeniedException.class,
                () -> service.findHistoryByColdRoom(3L, null, NivelAcesso.OPERADOR, null, null));
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test
    void findCurrentByColdRoom_throwsNotFoundWhenColdRoomIsOutsideUsersCd() {
        when(repository.existsColdRoomByIdAndCd(3L, CD_ID, false)).thenReturn(false);

        assertThrows(ColdRoomNotFoundException.class,
                () -> service.findCurrentByColdRoom(3L, CD_ID, NivelAcesso.OPERADOR));
        verify(repository, never()).findCurrentByColdRoomIdAndCd(3L, CD_ID, false);
    }

    @Test
    void findHistoryByColdRoom_throwsNotFoundWhenColdRoomIsOutsideUsersCd() {
        when(repository.existsColdRoomByIdAndCd(3L, CD_ID, false)).thenReturn(false);

        assertThrows(ColdRoomNotFoundException.class,
                () -> service.findHistoryByColdRoom(3L, CD_ID, NivelAcesso.OPERADOR, null, null));
        verify(repository, never()).findHistoryByColdRoomIdAndCd(3L, CD_ID, false, null, null);
    }

    @Test
    void superAdminCanReadAcrossCdsWithoutCdAssignment() {
        when(repository.findByReadingIdAndCd(10, null, true))
                .thenReturn(List.<Object[]>of(row(10, 7, 3, 5)));
        when(repository.existsColdRoomByIdAndCd(3L, null, true)).thenReturn(true);
        when(repository.findAllWithColdRoomByCd(null, true))
                .thenReturn(List.<Object[]>of(row(10, 7, 3, 5)));
        when(repository.findCurrentByColdRoomIdAndCd(3L, null, true))
                .thenReturn(List.<Object[]>of(row(10, 7, 3, null)));
        when(repository.findHistoryByColdRoomIdAndCd(3L, null, true, null, null))
                .thenReturn(List.<Object[]>of(row(10, 7, 3, 5)));

        assertEquals(1, service.findAll(null, NivelAcesso.SUPER_ADMIN).size());
        assertEquals(10L, service.findById(10, null, NivelAcesso.SUPER_ADMIN).readingId());
        assertEquals(10L, service.findCurrentByColdRoom(3L, null, NivelAcesso.SUPER_ADMIN)
                .orElseThrow().readingId());
        assertEquals(1, service.findHistoryByColdRoom(3L, null, NivelAcesso.SUPER_ADMIN, null, null).size());

        verify(repository).findAllWithColdRoomByCd(null, true);
        verify(repository).findByReadingIdAndCd(10, null, true);
        verify(repository).findCurrentByColdRoomIdAndCd(3L, null, true);
        verify(repository).findHistoryByColdRoomIdAndCd(3L, null, true, null, null);
    }

    private TemperatureReadingRequest request() {
        return new TemperatureReadingRequest(
                7L,
                new BigDecimal("-12.50"),
                OffsetDateTime.parse("2026-10-03T18:00:00-03:00")
        );
    }

    private User adminUser() {
        User user = new User();
        user.setId(9L);
        user.setNivelAcesso(NivelAcesso.ADMIN);
        return user;
    }

    private void givenRedisStreamOperations() {
        doReturn(streamOperations).when(redisTemplate).opsForStream();
    }

    private Object[] row(Integer readingId, Integer thermometerId, Integer coldRoomId, Integer alertId) {
        return new Object[]{
                readingId,
                thermometerId,
                coldRoomId,
                alertId,
                new BigDecimal("-12.50"),
                MEASURED_AT,
                "evt-123"
        };
    }
}
