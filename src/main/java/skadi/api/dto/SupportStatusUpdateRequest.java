package skadi.api.dto;

import jakarta.validation.constraints.NotNull;
import skadi.api.enums.SupportStatus;

public record SupportStatusUpdateRequest(
        @NotNull(message = "O status do chamado é obrigatório")
        SupportStatus status
) {
}
