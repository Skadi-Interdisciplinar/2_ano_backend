package skadi.api.mapper;

import skadi.api.dto.RegisterRequest;
import skadi.api.dto.RegisterResponse;
import skadi.api.model.User;

public class UserMapper {
    public static User fromDTO(RegisterRequest dto){
        return new User(dto.nome(), dto.username(), dto.cpf(), dto.email(), dto.senha(), dto.nivelAcesso(), dto.codCD(), dto.codGestor());
    }

    public static RegisterResponse toDTO(User user){
    return new RegisterResponse(user.getId(),user.getNome(),user.getUsername(),user.getEmail(),user.getNivelAcesso(), user.getCodCD(), user.getCodGestor());
    }

}
