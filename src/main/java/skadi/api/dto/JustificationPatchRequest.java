package skadi.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JustificationPatchRequest(
        @Size(max = 255, message = "O motivo deve ter no máximo 255 caracteres.")
        @Pattern(regexp = "(?s).*\\S.*", message = "O motivo não pode ficar em branco.")
        String reason,
        @Pattern(regexp = "(?s).*\\S.*", message = "A descrição não pode ficar em branco.")
        String description
) {}
