package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import skadi.api.repository.TemperatureReadingRepository;
import org.springframework.test.util.ReflectionTestUtils;
import skadi.api.dto.TemperatureReadingRequest;
import skadi.api.dto.TemperatureReadingResponse;
import skadi.api.dto.TemperatureReadingView;

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

@ExtendWith(MockitoExtension.class)
class TemperatureReadingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, String, String> streamOperations;

    @Mock
    private TemperatureReadingRepository repository;

    private TemperatureReadingService service;

    @BeforeEach
    void setUp() {
        service = new TemperatureReadingService(redisTemplate, repository);
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

        TemperatureReadingResponse response = service.publish(request);

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

        TemperatureReadingResponse first = service.publish(request);
        TemperatureReadingResponse second = service.publish(request);

        assertEquals(first.eventId(), second.eventId());
    }

    @Test
    void publish_trimsRedisStreamUsingConfiguredRetention() {
        givenRedisStreamOperations();
        when(streamOperations.add(any(MapRecord.class)))
                .thenReturn(RecordId.of("1740000000000-0"));

        service.publish(request());

        verify(streamOperations).trim(eq("skadi:leituras:temperatura"), any(RedisStreamCommands.XTrimOptions.class));
    }

    @Test
    void publish_doesNotTrimWhenRedisPublishFails() {
        givenRedisStreamOperations();
        when(streamOperations.add(any(MapRecord.class)))
                .thenThrow(new IllegalStateException("Redis indisponível"));

        assertThrows(IllegalStateException.class, () -> service.publish(request()));

        verify(streamOperations, never())
                .trim(any(String.class), any(RedisStreamCommands.XTrimOptions.class));
    }

    @Test
    void findAll_mapsRepositoryRowsToViews() {
        when(repository.findAllWithColdRoom()).thenReturn(List.<Object[]>of(row(10, 7, 3, 5)));

        List<TemperatureReadingView> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).readingId());
        assertEquals(7L, result.get(0).thermometerId());
        assertEquals(3L, result.get(0).coldRoomId());
        assertEquals(5L, result.get(0).alertId());
        assertEquals(new BigDecimal("-12.50"), result.get(0).temperature());
    }

    @Test
    void findCurrentByColdRoom_returnsMappedMostRecentReading() {
        when(repository.findCurrentByColdRoomId(3L)).thenReturn(List.<Object[]>of(row(10, 7, 3, null)));

        Optional<TemperatureReadingView> result = service.findCurrentByColdRoom(3L);

        assertEquals(10L, result.orElseThrow().readingId());
        assertEquals(3L, result.orElseThrow().coldRoomId());
        assertEquals(null, result.orElseThrow().alertId());
    }

    @Test
    void findCurrentByColdRoom_returnsEmptyWhenRepositoryHasNoReading() {
        when(repository.findCurrentByColdRoomId(3L)).thenReturn(List.of());

        assertEquals(Optional.empty(), service.findCurrentByColdRoom(3L));
    }

    @Test
    void findHistoryByColdRoom_forwardsPeriodToRepositoryAndMapsRows() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 4, 23, 59);
        when(repository.findHistoryByColdRoomId(3L, from, to))
                .thenReturn(List.<Object[]>of(row(10, 7, 3, 5)));

        List<TemperatureReadingView> result = service.findHistoryByColdRoom(3L, from, to);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).readingId());
        verify(repository).findHistoryByColdRoomId(3L, from, to);
    }

    private TemperatureReadingRequest request() {
        return new TemperatureReadingRequest(
                7L,
                new BigDecimal("-12.50"),
                OffsetDateTime.parse("2026-10-03T18:00:00-03:00")
        );
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
                Timestamp.valueOf("2026-10-04 10:00:00"),
                "evt-123"
        };
    }
}
