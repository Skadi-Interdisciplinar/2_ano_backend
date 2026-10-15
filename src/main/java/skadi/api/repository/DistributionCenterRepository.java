package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skadi.api.model.DistributionCenter;

import java.util.Optional;

@Repository

public interface DistributionCenterRepository extends JpaRepository<DistributionCenter, Long> {

    Optional<DistributionCenter> findById(Long id);

    boolean existsByCnpj(String cnpj);

}
