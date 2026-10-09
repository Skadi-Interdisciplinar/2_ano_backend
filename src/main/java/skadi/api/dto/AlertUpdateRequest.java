package skadi.api.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AlertUpdateRequest(
        @NotNull Integer coldRoomId,
        BigDecimal referenceShelfLifeHours,
        @NotNull String type
) {}
