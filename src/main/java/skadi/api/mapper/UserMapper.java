package skadi.api.mapper;

import skadi.api.dto.RegisterRequest;
import skadi.api.dto.RegisterResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;

public class UserMapper {
    public static User fromDTO(RegisterRequest dto){
        if (dto.nivelAcesso() == NivelAcesso.ADMIN) {
            return new User(
                    dto.nome(),
                    dto.username(),
                    dto.cpf(),
                    dto.email(),
                    dto.senha(),
                    dto.nivelAcesso(),
                    dto.cd(),
                    dto.codGestor()
            );
        }

        DistributionCenter uniqueCd = null;
        if (dto.nivelAcesso() == NivelAcesso.GESTOR || dto.nivelAcesso() == NivelAcesso.OPERADOR) {
            uniqueCd = dto.cdGestor();
        }

        return new User(
                dto.nome(),
                dto.username(),
                dto.cpf(),
                dto.email(),
                dto.senha(),
                dto.nivelAcesso(),
                uniqueCd,
                dto.codGestor()
        );
    }

    public static RegisterResponse toDTO(User user){
        return new RegisterResponse(
                user.getId(),
                user.getNome(),
                user.getUsername(),
                user.getEmail(),
                user.getNivelAcesso(),
                user.getCdGestor(),
                user.getCd(),
                user.getCodGestor()
        );
    }

}
