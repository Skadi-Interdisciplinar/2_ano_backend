package skadi.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "A senha atual é obrigatória")
        String senhaAtual,

        @NotBlank(message = "A nova senha é obrigatória")
        String novaSenha,

        @NotBlank(message = "A confirmação da senha é obrigatória")
        @JsonAlias({"confirmarNovaSenha", "confirmacaoNovaSenha"})
        String confirmacaoSenha
) {
}
