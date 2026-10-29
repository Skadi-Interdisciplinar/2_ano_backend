package skadi.api.dto;

import skadi.api.enums.NivelAcesso;

import java.util.List;

public record UserResponse(
        Long id,
        String nome,
        String username,
        String cpf,
        String email,
        NivelAcesso nivelAcesso,
        DistributionCenterResponse cdGestor,
        List<DistributionCenterResponse> cd,
        Integer codGestor
) {
}
