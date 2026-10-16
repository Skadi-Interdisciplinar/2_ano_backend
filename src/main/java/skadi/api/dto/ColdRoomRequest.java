package skadi.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ColdRoomRequest(
        @NotBlank(message = "O modelo da câmara frigorífica é obrigatório")
        @Size(max = 150, message = "O modelo deve ter no máximo 150 caracteres")
        String model,

        @NotBlank(message = "A localização da câmara frigorífica é obrigatória")
        @Size(max = 100, message = "A localização deve ter no máximo 100 caracteres")
        String location,

        @NotNull(message = "A temperatura mínima é obrigatória")
        @Digits(integer = 3, fraction = 2,
                message = "A temperatura mínima deve ter até 3 dígitos inteiros e 2 decimais")
        BigDecimal minimumTemperature,

        @NotNull(message = "A temperatura máxima é obrigatória")
        @Digits(integer = 3, fraction = 2,
                message = "A temperatura máxima deve ter até 3 dígitos inteiros e 2 decimais")
        BigDecimal maximumTemperature,

        @NotNull(message = "O centro de distribuição é obrigatório")
        @Positive(message = "O centro de distribuição deve possuir um identificador válido")
        Long distributionCenterId,

        @NotNull(message = "O termômetro é obrigatório")
        @Positive(message = "O termômetro deve possuir um identificador válido")
        Long thermometerId
) {
}
