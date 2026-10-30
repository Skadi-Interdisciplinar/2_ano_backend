package skadi.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail é inválido")
        String email,

        @NotBlank(message = "O código é obrigatório")
        String codigo,

        @NotBlank(message = "A senha é obrigatória")
        String senha
) {
}
