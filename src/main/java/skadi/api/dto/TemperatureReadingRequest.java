package skadi.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TemperatureReadingRequest(
        @NotNull(message = "thermometerId é obrigatório")
        Long thermometerId,

        @NotNull(message = "temperature é obrigatória")
        @DecimalMin(value = "-100.0", message = "temperature deve ser maior ou igual a -100 °C")
        @DecimalMax(value = "100.0", message = "temperature deve ser menor ou igual a 100 °C")
        BigDecimal temperature,

        @NotNull(message = "measuredAt é obrigatória")
        OffsetDateTime measuredAt
) {
}
