package skadi.api.dto;

import java.time.LocalDateTime;

public record JustificationResponse(
        Integer id,
        Integer alertHandlingId,
        String reason,
        String description,
        LocalDateTime createdAt
) {}
