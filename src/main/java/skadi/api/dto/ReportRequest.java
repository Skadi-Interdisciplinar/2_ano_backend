package skadi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record ReportRequest(
        @NotBlank String content,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotNull @Positive Integer distributionCenterId
) {
}
