package skadi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import skadi.api.enums.SupportCategory;

public record SupportRequestUpdateRequest(
        @NotNull(message = "A categoria é obrigatória")
        SupportCategory categoria,

        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        String resposta
) {
}
