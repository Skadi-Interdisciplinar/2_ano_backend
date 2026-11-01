package skadi.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ColdRoomRequest(
        @NotBlank(message = "A localizaǜo da cǽmara frigorfica Ǹ obrigatria")
        @Size(max = 100, message = "A localizaǜo deve ter no mǭximo 100 caracteres")
        String location,

        @NotNull(message = "A temperatura mnima Ǹ obrigatria")
        @Digits(integer = 3, fraction = 2,
                message = "A temperatura mnima deve ter atǸ 3 dgitos inteiros e 2 decimais")
        BigDecimal minimumTemperature,

        @NotNull(message = "A temperatura mǭxima Ǹ obrigatria")
        @Digits(integer = 3, fraction = 2,
                message = "A temperatura mǭxima deve ter atǸ 3 dgitos inteiros e 2 decimais")
        BigDecimal maximumTemperature,

        @NotNull(message = "O centro de distribuiǜo Ǹ obrigatrio")
        @Positive(message = "O centro de distribuiǜo deve possuir um identificador vǭlido")
        Long distributionCenterId,

        @NotNull(message = "O termmetro Ǹ obrigatrio")
        @Positive(message = "O termmetro deve possuir um identificador vǭlido")
        Long thermometerId
) {
}
