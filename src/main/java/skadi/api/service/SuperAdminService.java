package skadi.api.service;


import org.springframework.stereotype.Service;
import skadi.api.dto.UserRequest;
import skadi.api.dto.UserResponse;
import skadi.api.enums.NivelAcesso;
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

    public void updateAdmin(UserRequest request) {
        User user = repository.findById(request.id()).orElseThrow(() -> {
            throw new UserNotFoundException("Usuário não encontrado");
        });


        




    }
}
