package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skadi.api.model.User;
import skadi.api.enums.NivelAcesso;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByCpfIgnoreCase(String cpf);

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    boolean existsByNivelAcessoAndCdGestor_Id(NivelAcesso nivelAcesso, Long distributionCenterId);

    boolean existsByIdAndNivelAcessoAndCdGestor_Id(Long id, NivelAcesso nivelAcesso, Long distributionCenterId);

    List<User> findAllByNivelAcesso(NivelAcesso acesso);
}
