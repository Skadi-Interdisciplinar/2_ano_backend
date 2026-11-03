package skadi.api.service;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final JdbcTemplate jdbcTemplate;

    public SuperAdminService(UserRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllAdmins(){
        return UserMapper.toResponseList(repository.findAllByNivelAcesso(NivelAcesso.ADMIN));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        if (request.nivelAcesso() != user.getNivelAcesso()) {
            throw new AccessLevelChangeNotAllowedException();
        }

        UserMapper.update(user, request);
        if (user.getGestor() != null && user.getGestor().getId() != null) {
            user.setGestor(repository.getReferenceById(user.getGestor().getId()));
        }
        user.validateGestorAssociation();
        return UserMapper.toResponse(repository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));
        releaseHistory(id);
        if (user.getCd() != null) {
            user.getCd().clear();
        }
        repository.findAllByGestor_Id(id).forEach(linked -> linked.setGestor(null));
        repository.delete(user);
    }

    private void releaseHistory(Long id) {
        jdbcTemplate.update("UPDATE tb_log_acesso SET cod_usuario = NULL WHERE cod_usuario = ?", id);
        jdbcTemplate.update("UPDATE tb_solicitacao_suporte SET cod_usuario_responsavel = NULL WHERE cod_usuario_responsavel = ?", id);
        jdbcTemplate.update("DELETE FROM tb_solicitacao_suporte WHERE cod_usuario_solicitante = ?", id);
        jdbcTemplate.update("UPDATE tb_atendimento SET cod_usuario = NULL WHERE cod_usuario = ?", id);
        jdbcTemplate.update("DELETE FROM tb_notificacao_alerta WHERE cod_usuario = ?", id);
        jdbcTemplate.update("""
                DELETE FROM tb_assinatura
                WHERE cod_relatorio IN (SELECT id FROM tb_relatorio WHERE cod_usuario_gerador = ?)
                """, id);
        jdbcTemplate.update("DELETE FROM tb_relatorio WHERE cod_usuario_gerador = ?", id);
    }
}
