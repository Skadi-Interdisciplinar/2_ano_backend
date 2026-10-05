package skadi.api.dto;

import java.time.OffsetDateTime;

public record TemperatureReadingResponse(
        String eventId,
        String redisMessageId,
        String status,
        OffsetDateTime receivedAt
) {
}
