package skadi.api.dto;

import skadi.api.enums.NivelAcesso;
import skadi.api.model.DistributionCenter;

public record RegisterResponse(
        Long id,
        String nome,
        String username,
        String email,
        NivelAcesso nivelAcesso,
        DistributionCenter cd,
        Integer codGestor
) {
}
