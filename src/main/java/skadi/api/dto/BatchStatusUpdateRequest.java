package skadi.api.dto;

import jakarta.validation.constraints.NotNull;
import skadi.api.enums.BatchStatus;

public record BatchStatusUpdateRequest(
        @NotNull(message = "O status do lote é obrigatório")
        BatchStatus status
) {
}
