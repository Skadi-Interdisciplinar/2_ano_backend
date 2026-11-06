package skadi.api.mapper;

import skadi.api.dto.DistributionCenterResponse;
import skadi.api.dto.GestorResponse;
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
                    toGestor(dto.codGestor())
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
                toGestor(dto.codGestor())
        );
    }

    public static User update(User user, UserRequest request) {
        user.setNome(request.nome());
        user.setUsername(request.username());
        user.setCpf(request.cpf());
        user.setEmail(request.email());
        user.setNivelAcesso(request.nivelAcesso());
        user.setGestor(toGestor(request.codGestor()));
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
                toGestorResponse(user.getGestor())
        );
    }

    public static UserResponse toResponse(User user) {
        DistributionCenterResponse cdGestor = null;
        try {
            cdGestor = toDistributionCenterResponse(user.getCdGestor());
        } catch (Exception ignored) {
        }

        List<DistributionCenterResponse> cds = null;
        try {
            cds = toDistributionCenterResponses(user.getCd());
        } catch (Exception ignored) {
        }

        GestorResponse gestor = null;
        try {
            gestor = toGestorResponse(user.getGestor());
        } catch (Exception ignored) {
        }

        return new UserResponse(
                user.getId(),
                user.getNome(),
                user.getUsername(),
                user.getCpf(),
                user.getEmail(),
                user.getNivelAcesso(),
                cdGestor,
                cds,
                gestor
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

    private static User toGestor(GestorResponse gestor) {
        if (gestor == null || gestor.id() == null) {
            return null;
        }
        User user = new User();
        user.setId(gestor.id());
        user.setNome(gestor.nome());
        user.setUsername(gestor.username());
        user.setEmail(gestor.email());
        user.setNivelAcesso(gestor.nivelAcesso());
        return user;
    }

    private static GestorResponse toGestorResponse(User gestor) {
        if (gestor == null) {
            return null;
        }
        try {
            if (gestor.getId() == null) {
                return null;
            }
            return new GestorResponse(
                    gestor.getId(),
                    gestor.getNome(),
                    gestor.getUsername(),
                    gestor.getEmail(),
                    gestor.getNivelAcesso()
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<DistributionCenterResponse> toDistributionCenterResponses(List<DistributionCenter> cds) {
        if (cds == null) {
            return null;
        }
        try {
            return DistributionCenterMapper.toDTOList(cds);
        } catch (Exception ignored) {
            return null;
        }
    }

}
