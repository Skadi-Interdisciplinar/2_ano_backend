package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skadi.api.model.Justification;

import java.util.List;
import java.util.Optional;

public interface JustificationRepository extends JpaRepository<Justification, Integer> {
    List<Justification> findByAlertHandlingIdOrderByCreatedAtDesc(Integer alertHandlingId);
    Optional<Justification> findByIdAndAlertHandlingId(Integer id, Integer alertHandlingId);
}
