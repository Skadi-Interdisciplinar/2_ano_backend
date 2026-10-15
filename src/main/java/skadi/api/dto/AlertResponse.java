package skadi.api.dto;

import skadi.api.enums.AlertStatus;
import skadi.api.enums.CurrentLevel;
import skadi.api.enums.SeverityLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlertResponse(
        Integer id,
        Integer coldRoomId,
        BigDecimal referenceShelfLifeHours,
        CurrentLevel currentLevel,
        LocalDateTime createdAt,
        String type,
        SeverityLevel severityLevel,
        AlertStatus status
) {
}
