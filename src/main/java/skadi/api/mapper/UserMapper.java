package skadi.api.mapper;

import skadi.api.dto.DistributionCenterResponse;
import skadi.api.dto.RegisterRequest;
import skadi.api.dto.RegisterResponse;
import skadi.api.dto.UserRequest;
import skadi.api.dto.UserResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;

import java.util.List;

public final class UserMapper {
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

    public static User update(User user, UserRequest request) {
        user.setNome(request.nome());
        user.setUsername(request.username());
        user.setCpf(request.cpf());
        user.setEmail(request.email());
        user.setNivelAcesso(request.nivelAcesso());
        user.setCodGestor(request.codGestor());
        return user;
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

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getNome(),
                user.getUsername(),
                user.getCpf(),
                user.getEmail(),
                user.getNivelAcesso(),
                toDistributionCenterResponse(user.getCdGestor()),
                toDistributionCenterResponses(user.getCd()),
                user.getCodGestor()
        );
    }

    public static List<UserResponse> toResponseList(List<User> users) {
        return users.stream().map(UserMapper::toResponse).toList();
    }

    private static DistributionCenterResponse toDistributionCenterResponse(DistributionCenter cd) {
        if (cd == null) {
            return null;
        }
        return DistributionCenterMapper.toDTO(cd);
    }

    private static List<DistributionCenterResponse> toDistributionCenterResponses(List<DistributionCenter> cds) {
        if (cds == null) {
            return null;
        }
        return DistributionCenterMapper.toDTOList(cds);
    }

}
