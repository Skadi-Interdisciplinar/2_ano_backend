package skadi.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriorityAlertDto(
        Integer id,
        Long coldRoomId,
        String coldRoomName,
        BigDecimal currentTemperature,
        BigDecimal minimumTemperature,
        BigDecimal maximumTemperature,
        String severityLevel,
        String status,
        String currentLevel,
        LocalDateTime createdAt,
        Long remainingMinutes,
        Double percentConsumed,
        String slaStatus,
        String assigneeName
) {
}
