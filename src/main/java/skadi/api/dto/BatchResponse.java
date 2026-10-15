package skadi.api.dto;

import skadi.api.enums.BatchStatus;

import java.time.LocalDate;

public record BatchResponse(
        Long id,
        String batchCode,
        Long categoryId,
        String categoryName,
        LocalDate manufacturingDate,
        LocalDate expirationDate,
        BatchStatus status
) {
}
