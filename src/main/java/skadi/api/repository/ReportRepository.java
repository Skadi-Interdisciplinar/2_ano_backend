package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skadi.api.model.Report;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Integer> {

    @Query(value = """
            SELECT r.*
            FROM tb_relatorio r
            JOIN tb_usuario u ON u.id = r.cod_usuario_gerador
            WHERE u.cod_cd = :cdId
            ORDER BY r.data_hora_geracao DESC
            """, nativeQuery = true)
    List<Report> findAllByAuthorCdOrderByGeneratedAtDesc(@Param("cdId") Integer cdId);

    @Query(value = """
            SELECT r.*
            FROM tb_relatorio r
            JOIN tb_usuario u ON u.id = r.cod_usuario_gerador
            WHERE r.id = :reportId AND u.cod_cd = :cdId
            """, nativeQuery = true)
    Optional<Report> findByIdAndAuthorCd(
            @Param("reportId") Integer reportId,
            @Param("cdId") Integer cdId
    );

    Optional<Report> findByIdAndGeneratedByUserId(Integer id, Integer generatedByUserId);
}
