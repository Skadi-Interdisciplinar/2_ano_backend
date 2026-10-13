package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skadi.api.model.TemperatureReading;

import java.time.LocalDateTime;
import java.util.List;

public interface TemperatureReadingRepository extends JpaRepository<TemperatureReading, Integer> {

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_camara_frigorifica c
                WHERE c.id = :coldRoomId
                  AND (:allCds = true OR c.cod_cd = :cdId)
            )
            """, nativeQuery = true)
    boolean existsColdRoomByIdAndCd(
            @Param("coldRoomId") Long coldRoomId,
            @Param("cdId") Integer cdId,
            @Param("allCds") boolean allCds
    );

    @Query(value = """
            SELECT l.id, l.cod_termometro, c.id AS cold_room_id, l.cod_alerta,
                   l.temperatura, l.data_hora, l.id_evento_redis
            FROM tb_leitura_temperatura l
            JOIN tb_camara_frigorifica c ON c.cod_termometro = l.cod_termometro
            WHERE (:allCds = true OR c.cod_cd = :cdId)
              AND (l.temperatura < c.temperatura_min
                   OR l.temperatura > c.temperatura_max)
            ORDER BY l.data_hora DESC
            """, nativeQuery = true)
    List<Object[]> findAllWithColdRoomByCd(
            @Param("cdId") Integer cdId,
            @Param("allCds") boolean allCds
    );

    @Query(value = """
            SELECT l.id, l.cod_termometro, c.id AS cold_room_id, l.cod_alerta,
                   l.temperatura, l.data_hora, l.id_evento_redis
            FROM tb_leitura_temperatura l
            JOIN tb_camara_frigorifica c ON c.cod_termometro = l.cod_termometro
            WHERE l.id = :readingId
              AND (:allCds = true OR c.cod_cd = :cdId)
            """, nativeQuery = true)
    List<Object[]> findByReadingIdAndCd(
            @Param("readingId") Integer readingId,
            @Param("cdId") Integer cdId,
            @Param("allCds") boolean allCds
    );

    @Query(value = """
            SELECT l.id, l.cod_termometro, c.id AS cold_room_id, l.cod_alerta,
                   l.temperatura, l.data_hora, l.id_evento_redis
            FROM tb_leitura_temperatura l
            JOIN tb_camara_frigorifica c ON c.cod_termometro = l.cod_termometro
            WHERE c.id = :coldRoomId
              AND (:allCds = true OR c.cod_cd = :cdId)
            ORDER BY l.data_hora DESC
            LIMIT 1
            """, nativeQuery = true)
    List<Object[]> findCurrentByColdRoomIdAndCd(
            @Param("coldRoomId") Long coldRoomId,
            @Param("cdId") Integer cdId,
            @Param("allCds") boolean allCds
    );

    @Query(value = """
            SELECT l.id, l.cod_termometro, c.id AS cold_room_id, l.cod_alerta,
                   l.temperatura, l.data_hora, l.id_evento_redis
            FROM tb_leitura_temperatura l
            JOIN tb_camara_frigorifica c ON c.cod_termometro = l.cod_termometro
            WHERE c.id = :coldRoomId
              AND (:allCds = true OR c.cod_cd = :cdId)
              AND (
                    l.temperatura < c.temperatura_min
                    OR l.temperatura > c.temperatura_max
              )
              AND ((:fromDate)::timestamp IS NULL
                   OR l.data_hora >= (:fromDate)::timestamp)
              AND ((:toDate)::timestamp IS NULL
                   OR l.data_hora <= (:toDate)::timestamp)
            ORDER BY l.data_hora DESC
            """, nativeQuery = true)
    List<Object[]> findHistoryByColdRoomIdAndCd(
            @Param("coldRoomId") Long coldRoomId,
            @Param("cdId") Integer cdId,
            @Param("allCds") boolean allCds,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

}
