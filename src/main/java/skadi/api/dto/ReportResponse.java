package skadi.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import skadi.api.enums.ReportStatus;

public record ReportResponse(
        Integer id,
        Integer generatedByUserId,
        LocalDateTime generatedAt,
        String contentHash,
        LocalDate periodStart,
        LocalDate periodEnd,
        ReportStatus status,
        String content
) {
}
