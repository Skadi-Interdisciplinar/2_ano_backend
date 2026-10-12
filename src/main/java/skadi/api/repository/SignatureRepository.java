package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skadi.api.model.Signature;

import java.util.List;
import java.util.Optional;

public interface SignatureRepository extends JpaRepository<Signature, Integer> {

    @Query(value = """
            SELECT s.*
            FROM tb_assinatura s
            JOIN tb_relatorio r ON r.id = s.cod_relatorio
            JOIN tb_usuario u ON u.id = r.cod_usuario_gerador
            WHERE u.cod_cd = :cdId
            ORDER BY s.id DESC
            """, nativeQuery = true)
    List<Signature> findAllByReportAuthorCdOrderByIdDesc(@Param("cdId") Integer cdId);

    @Query(value = """
            SELECT s.*
            FROM tb_assinatura s
            JOIN tb_relatorio r ON r.id = s.cod_relatorio
            JOIN tb_usuario u ON u.id = r.cod_usuario_gerador
            WHERE s.cod_relatorio = :reportId AND u.cod_cd = :cdId
            """, nativeQuery = true)
    Optional<Signature> findByReportIdAndAuthorCd(
            @Param("reportId") Integer reportId,
            @Param("cdId") Integer cdId
    );

    @Query(value = """
            SELECT s.*
            FROM tb_assinatura s
            JOIN tb_relatorio r ON r.id = s.cod_relatorio
            JOIN tb_usuario u ON u.id = r.cod_usuario_gerador
            WHERE s.id = :signatureId AND u.cod_cd = :cdId
            """, nativeQuery = true)
    Optional<Signature> findByIdAndReportAuthorCd(
            @Param("signatureId") Integer signatureId,
            @Param("cdId") Integer cdId
    );

    boolean existsByReportId(Integer reportId);
}
