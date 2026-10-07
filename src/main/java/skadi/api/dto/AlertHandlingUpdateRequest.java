package skadi.api.dto;

import jakarta.validation.constraints.NotNull;
import skadi.api.enums.AlertHandlingStatus;

public record AlertHandlingUpdateRequest(@NotNull AlertHandlingStatus status) {}
