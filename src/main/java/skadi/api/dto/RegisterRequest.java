package skadi.api.dto;

import skadi.api.enums.NivelAcesso;

public record RegisterRequest(
        String nome,
        String username,
        String cpf,
        String email,
        String senha,
        NivelAcesso nivelAcesso,
        Integer codCD
) {
}
