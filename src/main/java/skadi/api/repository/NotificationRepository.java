package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skadi.api.model.AlertNotification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<AlertNotification, Integer> {
    List<AlertNotification> findAllByUserIdOrderBySentAtDesc(Long userId);

    Optional<AlertNotification> findByIdAndUserId(Integer id, Long userId);
}
