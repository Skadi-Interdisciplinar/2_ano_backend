package skadi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter exatamente 8 números")
        String zipCode,

        @NotBlank(message = "A rua é obrigatória")
        @Size(max = 150, message = "A rua deve ter no máximo 150 caracteres")
        String street,

        @NotNull(message = "O número é obrigatório")
        @PositiveOrZero(message = "O número não pode ser negativo")
        Integer number,

        @NotBlank(message = "A cidade é obrigatória")
        @Size(max = 60, message = "A cidade deve ter no máximo 60 caracteres")
        String city,

        @NotBlank(message = "O bairro é obrigatório")
        @Size(max = 70, message = "O bairro deve ter no máximo 70 caracteres")
        String neighborhood,

        @Size(max = 50, message = "O complemento deve ter no máximo 50 caracteres")
        String complement,

        @NotNull(message = "O estado é obrigatório")
        @Positive(message = "O id do estado deve ser positivo")
        Long stateId
) {
}
