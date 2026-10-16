package skadi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ThermometerRequest(
        @NotBlank(message = "O modelo do termômetro é obrigatório")
        @Size(max = 150, message = "O modelo deve ter no máximo 150 caracteres")
        String model
) {
}
