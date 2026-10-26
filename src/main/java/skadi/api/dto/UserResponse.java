package skadi.api.dto;

import skadi.api.enums.NivelAcesso;
import skadi.api.model.DistributionCenter;

import java.util.List;

public record UserResponse(Long id,
                           String nome,
                           String username,
                           String email,
                           NivelAcesso nivelAcesso,
                           List<DistributionCenter> cd
                           ) {
}
