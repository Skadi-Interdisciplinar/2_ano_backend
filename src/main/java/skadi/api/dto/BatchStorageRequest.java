package skadi.api.dto;

import jakarta.validation.constraints.NotNull;

public record BatchStorageRequest(
        @NotNull(message = "A câmara frigorífica é obrigatória")
        Long coldRoomId
) {
}
