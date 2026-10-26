package skadi.api.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import skadi.api.config.TokenProvider;
import skadi.api.dto.*;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.UserAlreadyExistsException;
import skadi.api.exceptions.UserNotFoundException;
import skadi.api.mapper.UserMapper;
import skadi.api.model.User;
import skadi.api.repository.UserRepository;

import java.sql.Timestamp;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration}")
    private Long expirationTime;





    public void register(RegisterRequest request){
        if (request.nivelAcesso() != NivelAcesso.ADMIN) {
            throw new AccessDeniedException("SUPER_ADMIN cadastra apenas ADMIN");
        }

        if(repository.existsByUsername(request.username())){
            throw new UserAlreadyExistsException("Nome de usuário já utilizado");
        }

        User user = UserMapper.fromDTO(request);
        user.setSenha(passwordEncoder.encode(request.senha()));

        User saved = repository.save(user);

        return;


    }


    public TokenResponse login(LoginRequest dto) {
        try{
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.username(), dto.senha())
            );

            String token = tokenProvider.generateToken(authentication);

            return new TokenResponse(token, expirationTime, Timestamp.from(Instant.now().plusSeconds(expirationTime)));
    }catch (BadCredentialsException bad){
            throw new BadCredentialsException("Credenciais inválidas");
        }
    catch(Exception e){
            throw new RuntimeException("Erro inesperado: " + e);
    }
    }


    public AdminListResponse getAdminsSvc(){
        return UserMapper.toAdminListDTO(repository.findAllByNivelAcesso(NivelAcesso.ADMIN));
    }

    public UserResponse createAdmin(RegisterRequest request){
        ensureAdminRole(request);
        if (repository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Nome de usuário já utilizado");
        }
        if (repository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email já utilizado");
        }
        if (repository.existsByCpf(request.cpf())) {
            throw new UserAlreadyExistsException("Cpf já utilizado");
        }

        User user = UserMapper.fromDTO(request);
        user.setSenha(passwordEncoder.encode(request.senha()));
        return UserMapper.toUserDTO(repository.save(user));
    }

    public UserResponse updateAdmin(Long id, RegisterRequest request){
        ensureAdminRole(request);
        User admin = findAdminById(id);

        if (!admin.getUsername().equals(request.username()) && repository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Nome de usuário já utilizado");
        }
        if (!admin.getEmail().equals(request.email()) && repository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email já utilizado");
        }
        if (!admin.getCpf().equals(request.cpf()) && repository.existsByCpf(request.cpf())) {
            throw new UserAlreadyExistsException("Cpf já utilizado");
        }

        UserMapper.updateAdminFromDTO(admin, request);
        if (request.senha() != null && !request.senha().isBlank()) {
            admin.setSenha(passwordEncoder.encode(request.senha()));
        }
        return UserMapper.toUserDTO(repository.save(admin));
    }

    public void deleteAdmin(Long id){
        User admin = findAdminById(id);
        repository.delete(admin);
    }

    private User findAdminById(Long id) {
        return repository.findByIdAndNivelAcesso(id, NivelAcesso.ADMIN)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private void ensureAdminRole(RegisterRequest request) {
        if (request.nivelAcesso() != NivelAcesso.ADMIN) {
            throw new AccessDeniedException("SUPER_ADMIN cadastra apenas ADMIN");
        }
    }
}
