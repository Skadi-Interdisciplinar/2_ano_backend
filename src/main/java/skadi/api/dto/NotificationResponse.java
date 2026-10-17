package skadi.api.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Integer id,
        Integer alertId,
        Long userId,
        LocalDateTime sentAt,
        LocalDateTime readAt
) {}
