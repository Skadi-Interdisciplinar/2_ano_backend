package skadi.api.dto;

import java.math.BigDecimal;

public record CategoryResponse(
        Long id,
        String name,
        BigDecimal idealTemperature,
        BigDecimal shelfLifeHours
) {
}
