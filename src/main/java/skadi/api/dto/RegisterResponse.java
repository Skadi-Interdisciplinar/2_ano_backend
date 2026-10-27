package skadi.api.dto;

import skadi.api.enums.NivelAcesso;
import skadi.api.model.DistributionCenter;

import java.util.List;

public record RegisterResponse(
        Long id,
        String nome,
        String username,
        String email,
        NivelAcesso nivelAcesso,
        DistributionCenter cdGestor,
        List<DistributionCenter> cd,
        Integer codGestor
) {
}
