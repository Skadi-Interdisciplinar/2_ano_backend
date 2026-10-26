package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import skadi.api.dto.TemperatureReadingRequest;
import skadi.api.dto.TemperatureReadingResponse;
import skadi.api.dto.TemperatureReadingView;
import skadi.api.exceptions.ColdRoomNotFoundException;
import skadi.api.exceptions.TemperatureReadingNotFoundException;
import skadi.api.enums.NivelAcesso;
import skadi.api.repository.TemperatureReadingRepository;
import skadi.api.repository.ColdRoomRepository;
import skadi.api.repository.ThermometerRepository;
import skadi.api.model.User;
import skadi.api.exceptions.ThermometerNotFoundException;

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
    private final ColdRoomRepository coldRoomRepository;
    private final DistributionCenterAccessService accessService;
    private final ThermometerRepository thermometerRepository;

    @Value("${skadi.redis.temperature-stream}")
    private String temperatureStream;

    @Value("${skadi.redis.temperature-retention-hours:72}")
    private long temperatureRetentionHours;

    public TemperatureReadingResponse publish(TemperatureReadingRequest request, User user) {
        accessService.requireRole(user, NivelAcesso.ADMIN,
                "Somente ADMIN pode enviar leituras de temperatura.");
        Long distributionCenterId = thermometerRepository.findDistributionCenterId(request.thermometerId())
                .orElseThrow(() -> new ThermometerNotFoundException(request.thermometerId()));
        accessService.requireAdminOwnership(user, distributionCenterId);

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

    public List<TemperatureReadingView> findAll(Integer cdId, NivelAcesso accessLevel) {
        boolean allCds = hasGlobalReadAccess(cdId, accessLevel);
        return repository.findAllWithColdRoomByCd(cdId, allCds).stream()
                .map(this::toView)
                .toList();
    }

    public List<TemperatureReadingView> findAll(User user) {
        if (user != null && user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            return findAll(null, NivelAcesso.SUPER_ADMIN);
        }
        return accessService.accessibleDistributionCenterIds(user).stream()
                .flatMap(cdId -> repository.findAllWithColdRoomByCd(cdId.intValue(), false).stream())
                .map(this::toView)
                .distinct()
                .toList();
    }

    public TemperatureReadingView findById(Integer readingId, Integer cdId, NivelAcesso accessLevel) {
        boolean allCds = hasGlobalReadAccess(cdId, accessLevel);
        return repository.findByReadingIdAndCd(readingId, cdId, allCds).stream()
                .findFirst()
                .map(this::toView)
                .orElseThrow(() -> new TemperatureReadingNotFoundException(readingId));
    }

    public TemperatureReadingView findById(Integer readingId, User user) {
        if (user != null && user.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            return findById(readingId, null, NivelAcesso.SUPER_ADMIN);
        }
        return accessService.accessibleDistributionCenterIds(user).stream()
                .flatMap(cdId -> repository.findByReadingIdAndCd(
                        readingId, cdId.intValue(), false).stream())
                .findFirst()
                .map(this::toView)
                .orElseThrow(() -> new TemperatureReadingNotFoundException(readingId));
    }

    public Optional<TemperatureReadingView> findCurrentByColdRoom(Long coldRoomId, User user) {
        Integer cdId = coldRoomDistributionCenterId(coldRoomId, user);
        return repository.findCurrentByColdRoomIdAndCd(coldRoomId, cdId, false).stream()
                .findFirst()
                .map(this::toView);
    }

    public List<TemperatureReadingView> findHistoryByColdRoom(
            Long coldRoomId,
            User user,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Integer cdId = coldRoomDistributionCenterId(coldRoomId, user);
        return repository.findHistoryByColdRoomIdAndCd(coldRoomId, cdId, false, from, to).stream()
                .map(this::toView)
                .toList();
    }

    private Integer coldRoomDistributionCenterId(Long coldRoomId, User user) {
        skadi.api.model.ColdRoom coldRoom = coldRoomRepository.findById(coldRoomId)
                .orElseThrow(() -> new ColdRoomNotFoundException(coldRoomId));
        accessService.requireRead(user, coldRoom.getDistributionCenter().getId());
        return Math.toIntExact(coldRoom.getDistributionCenter().getId());
    }

    public Optional<TemperatureReadingView> findCurrentByColdRoom(
            Long coldRoomId,
            Integer cdId,
            NivelAcesso accessLevel
    ) {
        boolean allCds = validateColdRoomAccess(coldRoomId, cdId, accessLevel);
        return repository.findCurrentByColdRoomIdAndCd(coldRoomId, cdId, allCds).stream()
                .findFirst()
                .map(this::toView);
    }

    public List<TemperatureReadingView> findHistoryByColdRoom(
            Long coldRoomId,
            Integer cdId,
            NivelAcesso accessLevel,
            LocalDateTime from,
            LocalDateTime to
    ) {
        boolean allCds = validateColdRoomAccess(coldRoomId, cdId, accessLevel);
        return repository.findHistoryByColdRoomIdAndCd(coldRoomId, cdId, allCds, from, to).stream()
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
                toLocalDateTime(row[5]),
                (String) row[6]
        );
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        throw new IllegalArgumentException("Tipo de data/hora não suportado: " + value);
    }

    private boolean hasGlobalReadAccess(Integer cdId, NivelAcesso accessLevel) {
        if (accessLevel == NivelAcesso.SUPER_ADMIN) {
            return true;
        }
        if (cdId == null) {
            throw new AccessDeniedException("Usuário não está vinculado a um CD.");
        }
        return false;
    }

    private boolean validateColdRoomAccess(Long coldRoomId, Integer cdId, NivelAcesso accessLevel) {
        boolean allCds = hasGlobalReadAccess(cdId, accessLevel);
        if (!repository.existsColdRoomByIdAndCd(coldRoomId, cdId, allCds)) {
            throw new ColdRoomNotFoundException(coldRoomId);
        }
        return allCds;
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
