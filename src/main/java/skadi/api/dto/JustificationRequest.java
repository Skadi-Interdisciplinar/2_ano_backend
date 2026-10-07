package skadi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JustificationRequest(
        @NotBlank
        @Size(max = 255, message = "reason must contain at most 255 characters")
        String reason,
        @NotBlank String description
) {}
