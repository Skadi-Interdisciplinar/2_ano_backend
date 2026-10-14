package skadi.api.dto;

import skadi.api.enums.NivelAcesso;
import skadi.api.model.DistributionCenter;

public record RegisterRequest(
        String nome,
        String username,
        String cpf,
        String email,
        String senha,
        NivelAcesso nivelAcesso,
        DistributionCenter cd,
        Integer codGestor
) {
}
