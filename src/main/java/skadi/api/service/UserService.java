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
import org.springframework.transaction.annotation.Transactional;
import skadi.api.config.TokenProvider;
import skadi.api.dto.LoginRequest;
import skadi.api.dto.RegisterRequest;
import skadi.api.dto.RegisterResponse;
import skadi.api.dto.TokenResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.UserAlreadyExistsException;
import skadi.api.mapper.UserMapper;
import skadi.api.model.User;
import skadi.api.model.DistributionCenter;
import skadi.api.exceptions.CdNotFoundException;
import skadi.api.repository.DistributionCenterRepository;
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
    private final DistributionCenterRepository distributionCenterRepository;
    private final DistributionCenterAccessService accessService;
    private final AuditUserContext auditUserContext;

    @Value("${jwt.expiration}")
    private Long expirationTime;





    @Transactional
    public void register(RegisterRequest request, User authenticatedUser) {
        validateRegistrationPermission(request, authenticatedUser);
        validateUniqueFields(request);

        DistributionCenter distributionCenter = distributionCenterFor(request, authenticatedUser);
        validateManagerRules(request, distributionCenter);

        User user = UserMapper.fromDTO(request);
        if (authenticatedUser.getNivelAcesso() != NivelAcesso.SUPER_ADMIN) {
            user.setCdGestor(distributionCenter);
            user.setCd(null);
        }
        user.setSenha(passwordEncoder.encode(request.senha()));

        auditUserContext.setCurrentUser(authenticatedUser.getId());
        repository.save(user);
    }

    private void validateUniqueFields(RegisterRequest request) {
        if (repository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Nome de usuário já utilizado");
        }
        if (repository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("E-mail já utilizado");
        }
        if (request.cpf() != null && repository.existsByCpf(request.cpf())) {
            throw new UserAlreadyExistsException("CPF já utilizado");
        }
    }

    private void validateRegistrationPermission(RegisterRequest request, User authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getNivelAcesso() == null) {
            throw new AccessDeniedException("Usuário autenticado não identificado.");
        }

        if (authenticatedUser.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            if (request.nivelAcesso() != NivelAcesso.SUPER_ADMIN
                    && request.nivelAcesso() != NivelAcesso.ADMIN) {
                throw new AccessDeniedException(
                        "SUPER_ADMIN cadastra somente SUPER_ADMIN ou ADMIN.");
            }
            if (request.cdGestor() != null || request.codGestor() != null) {
                throw new AccessDeniedException(
                        "ADMIN e SUPER_ADMIN não podem possuir CD único ou gestor.");
            }
            return;
        }

        if (authenticatedUser.getNivelAcesso() == NivelAcesso.ADMIN) {
            if (request.nivelAcesso() != NivelAcesso.GESTOR
                    && request.nivelAcesso() != NivelAcesso.OPERADOR) {
                throw new AccessDeniedException("ADMIN cadastra somente GESTOR ou OPERADOR.");
            }
            Long cdId = uniqueCdId(request);
            if (cdId == null) {
                throw new AccessDeniedException("O CD do novo usuário é obrigatório.");
            }
            accessService.requireAdminOwnership(authenticatedUser, cdId);
            return;
        }

        throw new AccessDeniedException("O usuário não possui permissão para cadastrar usuários.");
    }

    private DistributionCenter distributionCenterFor(
            RegisterRequest request,
            User authenticatedUser
    ) {
        if (authenticatedUser.getNivelAcesso() == NivelAcesso.SUPER_ADMIN) {
            return null;
        }
        return distributionCenterRepository.findById(uniqueCdId(request))
                .orElseThrow(CdNotFoundException::new);
    }

    private Long uniqueCdId(RegisterRequest request) {
        if (request.cdGestor() == null || request.cdGestor().getId() == null) {
            return null;
        }
        return request.cdGestor().getId();
    }

    private void validateManagerRules(RegisterRequest request, DistributionCenter distributionCenter) {
        if (request.nivelAcesso() == NivelAcesso.GESTOR) {
            if (repository.existsByNivelAcessoAndCdGestor_Id(NivelAcesso.GESTOR, distributionCenter.getId())) {
                throw new UserAlreadyExistsException(
                        "O centro de distribuição já possui um gestor responsável");
            }
            if (request.codGestor() != null) {
                throw new AccessDeniedException("GESTOR não pode possuir outro gestor responsável.");
            }
        }

        if (request.nivelAcesso() == NivelAcesso.OPERADOR) {
            if (request.codGestor() == null || !repository.existsByIdAndNivelAcessoAndCdGestor_Id(
                    request.codGestor().longValue(), NivelAcesso.GESTOR, distributionCenter.getId())) {
                throw new AccessDeniedException(
                        "O operador deve possuir um gestor responsável do mesmo CD.");
            }
        }
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
