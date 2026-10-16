package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import skadi.api.model.ColdRoom;

@Repository
public interface ColdRoomRepository extends JpaRepository<ColdRoom, Long> {

    boolean existsByThermometer_Id(Long thermometerId);

    boolean existsByThermometer_IdAndIdNot(Long thermometerId, Long id);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_lote_camara_frigorifica
                WHERE cod_camara_frigorifica = :coldRoomId
            )
            """, nativeQuery = true)
    boolean hasBatchStorageHistory(@Param("coldRoomId") Long coldRoomId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_alerta
                WHERE cod_camara_frigorifica = :coldRoomId
            )
            """, nativeQuery = true)
    boolean hasAlerts(@Param("coldRoomId") Long coldRoomId);
}
