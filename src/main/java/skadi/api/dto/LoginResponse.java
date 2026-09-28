package skadi.api.dto;

import skadi.api.enums.NivelAcesso;

public record LoginResponse(
        String tipo,
        String token,
        Long id,
        String nome,
        String username,
        NivelAcesso nivelAcesso
) {
}
