package skadi.api.dto;

import java.time.LocalDate;

public record ReportPatchRequest(
        String content,
        LocalDate periodStart,
        LocalDate periodEnd
) {
}
