package skadi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import skadi.api.enums.SupportCategory;
import skadi.api.enums.SupportStatus;

public record SupportRequestRequest(
        @NotNull(message = "A categoria é obrigatória")
        SupportCategory categoria,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        SupportStatus status
) {
}
