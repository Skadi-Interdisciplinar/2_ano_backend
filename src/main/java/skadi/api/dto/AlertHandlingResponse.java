package skadi.api.dto;

import skadi.api.enums.AlertHandlingStatus;
import java.time.LocalDateTime;

public record AlertHandlingResponse(
        Integer id,
        Integer alertId,
        Long userId,
        LocalDateTime acknowledgedAt,
        LocalDateTime resolvedAt,
        AlertHandlingStatus status
) {}
