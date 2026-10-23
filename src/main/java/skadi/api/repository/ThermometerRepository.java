package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import skadi.api.model.Thermometer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ThermometerRepository extends JpaRepository<Thermometer, Long> {

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_camara_frigorifica
                WHERE cod_termometro = :thermometerId
            )
            """, nativeQuery = true)
    boolean isAssignedToColdRoom(@Param("thermometerId") Long thermometerId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_leitura_temperatura
                WHERE cod_termometro = :thermometerId
            )
            """, nativeQuery = true)
    boolean hasTemperatureReadings(@Param("thermometerId") Long thermometerId);

    @Query("""
            SELECT thermometer
            FROM Thermometer thermometer
            WHERE NOT EXISTS (
                SELECT coldRoom.id FROM ColdRoom coldRoom
                WHERE coldRoom.thermometer = thermometer
            ) OR EXISTS (
                SELECT coldRoom.id FROM ColdRoom coldRoom
                WHERE coldRoom.thermometer = thermometer
                  AND coldRoom.distributionCenter.id IN :distributionCenterIds
            )
            ORDER BY thermometer.model, thermometer.id
            """)
    List<Thermometer> findAccessible(
            @Param("distributionCenterIds") Collection<Long> distributionCenterIds);

    @Query("""
            SELECT coldRoom.distributionCenter.id
            FROM ColdRoom coldRoom
            WHERE coldRoom.thermometer.id = :thermometerId
            """)
    Optional<Long> findDistributionCenterId(@Param("thermometerId") Long thermometerId);
}
