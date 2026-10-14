package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skadi.api.model.Justification;

import java.util.Optional;

public interface JustificationRepository extends JpaRepository<Justification, Integer> {
    Optional<Justification> findByAlertHandlingId(Integer alertHandlingId);
    boolean existsByAlertHandlingId(Integer alertHandlingId);
}
