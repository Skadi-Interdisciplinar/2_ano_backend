package skadi.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordRecoveryRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail é inválido")
        String email
) {
}
