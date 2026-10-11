package skadi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReportRequest(
        @NotBlank String content,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd
) {
}
