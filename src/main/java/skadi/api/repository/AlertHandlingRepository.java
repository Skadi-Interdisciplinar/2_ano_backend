package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skadi.api.model.AlertHandling;

import java.util.List;
import java.util.Optional;

public interface AlertHandlingRepository extends JpaRepository<AlertHandling, Integer> {
    List<AlertHandling> findAllByOrderByIdDesc();
    Optional<AlertHandling> findFirstByAlertIdOrderByIdDesc(Integer alertId);
}
