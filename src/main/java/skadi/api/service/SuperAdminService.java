package skadi.api.service;


import org.springframework.stereotype.Service;
import skadi.api.dto.UserRequest;
import skadi.api.dto.UserResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.AccessLevelChangeNotAllowedException;
import skadi.api.exceptions.UserNotFoundException;
import skadi.api.mapper.UserMapper;
import skadi.api.model.User;
import skadi.api.repository.UserRepository;

import java.util.List;

@Service
public class SuperAdminService {
    private final UserRepository repository;

    public SuperAdminService(UserRepository repository) {
        this.repository = repository;
    }

    public List<UserResponse> getAllAdmins(){
        return UserMapper.toResponseList(repository.findAllByNivelAcesso(NivelAcesso.ADMIN));
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        if (request.nivelAcesso() != user.getNivelAcesso()) {
            throw new AccessLevelChangeNotAllowedException();
        }

        UserMapper.update(user, request);
        return UserMapper.toResponse(repository.save(user));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new UserNotFoundException("Usuário não encontrado");
        }
        repository.deleteById(id);
    }
}
