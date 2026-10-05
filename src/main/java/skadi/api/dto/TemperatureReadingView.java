package skadi.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TemperatureReadingView(
        Long readingId,
        Long thermometerId,
        Long coldRoomId,
        Long alertId,
        BigDecimal temperature,
        LocalDateTime measuredAt,
        String eventId
) {
}
