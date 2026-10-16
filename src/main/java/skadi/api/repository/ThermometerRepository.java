package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import skadi.api.model.Thermometer;

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
}
