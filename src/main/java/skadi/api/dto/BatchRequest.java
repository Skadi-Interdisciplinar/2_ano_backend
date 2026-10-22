package skadi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BatchRequest(
        @NotBlank(message = "O código do lote é obrigatório")
        @Size(max = 50, message = "O código do lote deve ter no máximo 50 caracteres")
        String batchCode,

        @NotNull(message = "A categoria é obrigatória")
        @Positive(message = "O id da categoria deve ser positivo")
        Long categoryId,

        @NotNull(message = "A data de fabricação é obrigatória")
        LocalDate manufacturingDate,

        @NotNull(message = "A data de validade é obrigatória")
        LocalDate expirationDate,

        @NotNull(message = "A câmara frigorífica inicial é obrigatória")
        @Positive(message = "O id da câmara frigorífica deve ser positivo")
        Long coldRoomId
) {
}
