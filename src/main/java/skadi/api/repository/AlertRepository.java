package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_alerta a
                JOIN tb_camara_frigorifica c ON c.id = a.cod_camara_frigorifica
                WHERE a.id = :alertId
                  AND c.cod_cd = :distributionCenterId
            )
            """, nativeQuery = true)
    boolean belongsToDistributionCenter(
            @Param("alertId") Integer alertId,
            @Param("distributionCenterId") Integer distributionCenterId
    );
}
