package skadi.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DistributionCenterRequest(
        @Size(max = 150,message = "Nome não pode passar de 150 carcteres")
        String name,
        @NotBlank(message = "Cnpj é obrigatória")
        @NotNull(message = "Cnpj é obrigatória")
        @Size(min = 14,max = 14, message = "CNPJ precisa de 14 carcteres")
        String cnpj
) {
}
