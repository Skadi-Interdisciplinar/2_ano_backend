package skadi.api.dto;

import jakarta.validation.constraints.NotNull;
import skadi.api.enums.ReportStatus;

public record ReportStatusUpdateRequest(@NotNull ReportStatus status) {
}
