package skadi.api.dto;

import jakarta.validation.constraints.Size;

public record DistributionCenterRequestName(
        @Size(max = 150,message = "Nome deve ser menor que 150 caracteres")
        String name
) {
}
