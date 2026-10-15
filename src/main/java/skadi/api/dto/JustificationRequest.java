package skadi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JustificationRequest(
        @NotBlank(message = "O motivo é obrigatório.")
        @Size(max = 255, message = "O motivo deve ter no máximo 255 caracteres.")
        String reason,
        @NotBlank(message = "A descrição é obrigatória.")
        String description
) {}
