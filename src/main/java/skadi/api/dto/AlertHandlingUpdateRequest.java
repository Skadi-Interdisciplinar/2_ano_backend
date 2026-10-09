package skadi.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import skadi.api.enums.AlertHandlingStatus;

public record AlertHandlingUpdateRequest(@NotNull AlertHandlingStatus status) {
    @AssertTrue(message = "RESOLVIDO status must be set through a justification")
    public boolean isStatusChangeAllowed() {
        return status != AlertHandlingStatus.RESOLVIDO;
    }
}
