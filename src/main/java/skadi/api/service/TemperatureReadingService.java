package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.stereotype.Service;
import skadi.api.dto.TemperatureReadingRequest;
import skadi.api.dto.TemperatureReadingResponse;
import skadi.api.dto.TemperatureReadingView;
import skadi.api.repository.TemperatureReadingRepository;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.Duration;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TemperatureReadingService {

    private final StringRedisTemplate redisTemplate;
    private final TemperatureReadingRepository repository;

    @Value("${skadi.redis.temperature-stream}")
    private String temperatureStream;

    @Value("${skadi.redis.temperature-retention-hours:72}")
    private long temperatureRetentionHours;

    public TemperatureReadingResponse publish(TemperatureReadingRequest request) {
        String eventId = eventIdFor(request);
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("id_evento_redis", eventId);
        fields.put("cod_termometro", request.thermometerId().toString());
        fields.put("temperatura", request.temperature().toPlainString());
        fields.put("data_hora", request.measuredAt().toString());

        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .in(temperatureStream)
                .ofMap(fields);
        RecordId messageId = redisTemplate.opsForStream().add(record);
        trimTemperatureStream();

        return new TemperatureReadingResponse(
                eventId,
                messageId.getValue(),
                "received",
                OffsetDateTime.now()
        );
    }

    private void trimTemperatureStream() {
        long cutoffTimestamp = System.currentTimeMillis()
                - Duration.ofHours(temperatureRetentionHours).toMillis();
        RedisStreamCommands.TrimOptions trimOptions = RedisStreamCommands.TrimOptions
                .minId(RecordId.of(cutoffTimestamp + "-0"))
                .exact();

        redisTemplate.opsForStream().trim(
                temperatureStream,
                RedisStreamCommands.XTrimOptions.of(trimOptions)
        );
    }

    public List<TemperatureReadingView> findAll() {
        return repository.findAllWithColdRoom().stream()
                .map(this::toView)
                .toList();
    }

    public Optional<TemperatureReadingView> findCurrentByColdRoom(Long coldRoomId) {
        return repository.findCurrentByColdRoomId(coldRoomId).stream()
                .findFirst()
                .map(this::toView);
    }

    public List<TemperatureReadingView> findHistoryByColdRoom(
            Long coldRoomId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return repository.findHistoryByColdRoomId(coldRoomId, from, to).stream()
                .map(this::toView)
                .toList();
    }

    private TemperatureReadingView toView(Object[] row) {
        return new TemperatureReadingView(
                ((Number) row[0]).longValue(),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                row[3] == null ? null : ((Number) row[3]).longValue(),
                (java.math.BigDecimal) row[4],
                ((Timestamp) row[5]).toLocalDateTime(),
                (String) row[6]
        );
    }

    private String eventIdFor(TemperatureReadingRequest request) {
        String canonicalReading = String.join("|",
                request.thermometerId().toString(),
                request.temperature().stripTrailingZeros().toPlainString(),
                request.measuredAt().toInstant().toString()
        );

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalReading.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder();
            for (byte value : digest) {
                hexadecimal.append(String.format("%02x", value));
            }
            return "evt-" + hexadecimal;
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o ID da leitura", exception);
        }
    }
}
