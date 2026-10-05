package skadi.api.dto;

import jakarta.validation.constraints.NotNull;
import skadi.api.enums.CurrentLevel;
import skadi.api.enums.SeverityLevel;

import java.math.BigDecimal;

public record AlertUpdateRequest(
        @NotNull Integer coldRoomId,
        BigDecimal referenceShelfLifeHours,
        @NotNull CurrentLevel currentLevel,
        @NotNull String type,
        @NotNull SeverityLevel severityLevel
) {}
