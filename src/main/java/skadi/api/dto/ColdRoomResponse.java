package skadi.api.dto;

import java.math.BigDecimal;

public record ColdRoomResponse(
        Long id,
        String model,
        String location,
        BigDecimal minimumTemperature,
        BigDecimal maximumTemperature,
        Long distributionCenterId,
        String distributionCenterName,
        Long thermometerId,
        String thermometerModel
) {
}
