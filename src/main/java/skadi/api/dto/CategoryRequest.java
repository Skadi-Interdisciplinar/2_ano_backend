package skadi.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CategoryRequest(
        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String name,

        @NotNull(message = "A temperatura mínima é obrigatória")
        @Digits(integer = 3, fraction = 2,
                message = "A temperatura mínima deve ter até 3 dígitos inteiros e 2 decimais")
        BigDecimal minimumTemperature,

        @NotNull(message = "A temperatura máxima é obrigatória")
        @Digits(integer = 3, fraction = 2,
                message = "A temperatura máxima deve ter até 3 dígitos inteiros e 2 decimais")
        BigDecimal maximumTemperature,

        @NotNull(message = "A vida útil em horas é obrigatória")
        @DecimalMin(value = "0.01", message = "A vida útil deve ser maior que zero")
        @DecimalMax(value = "999.99", message = "A vida útil deve ser menor que 1000 horas")
        @Digits(integer = 3, fraction = 2,
                message = "A vida útil deve ter até 3 dígitos inteiros e 2 decimais")
        BigDecimal shelfLifeHours
) {
}
