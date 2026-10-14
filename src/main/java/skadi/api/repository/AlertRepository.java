package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skadi.api.model.Alert;
import skadi.api.enums.AlertStatus;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Integer> {

    List<Alert> findAllByOrderByCreatedAtDesc();

    List<Alert> findByColdRoomIdOrderByCreatedAtDesc(Integer coldRoomId);

    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    List<Alert> findByColdRoomIdAndStatusOrderByCreatedAtDesc(
            Integer coldRoomId,
            AlertStatus status
    );
}
