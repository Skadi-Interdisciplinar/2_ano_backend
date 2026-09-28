package skadi.api.dto;

import skadi.api.enums.NivelAcesso;

public record RegisterResponse(
        Long id,
        String nome,
        String username,
        String email,
        NivelAcesso nivelAcesso,
        Integer codCD
) {
}
