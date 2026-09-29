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
import skadi.api.dto.LoginRequest;
import skadi.api.dto.RegisterRequest;
import skadi.api.dto.RegisterResponse;
import skadi.api.dto.TokenResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.UserAlreadyExistsException;
import skadi.api.mapper.UserMapper;
import skadi.api.model.User;
import skadi.api.repository.UserRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;

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
}
