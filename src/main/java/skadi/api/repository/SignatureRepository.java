package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skadi.api.model.Signature;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface SignatureRepository extends JpaRepository<Signature, Integer> {

    @Query(value = """
            SELECT s.* FROM tb_assinatura s
            JOIN tb_relatorio r ON r.id = s.cod_relatorio
            WHERE r.cod_cd IN (:cdIds)
            ORDER BY s.id DESC
            """, nativeQuery = true)
    List<Signature> findAllByReportCdIn(@Param("cdIds") Collection<Integer> cdIds);

    @Query(value = """
            SELECT s.* FROM tb_assinatura s
            JOIN tb_relatorio r ON r.id = s.cod_relatorio
            WHERE r.id = :reportId AND r.cod_cd IN (:cdIds)
            """, nativeQuery = true)
    Optional<Signature> findByReportIdAndReportCdIn(
            @Param("reportId") Integer reportId, @Param("cdIds") Collection<Integer> cdIds);

    @Query(value = """
            SELECT s.* FROM tb_assinatura s
            JOIN tb_relatorio r ON r.id = s.cod_relatorio
            WHERE s.id = :signatureId AND r.cod_cd IN (:cdIds)
            """, nativeQuery = true)
    Optional<Signature> findByIdAndReportCdIn(
            @Param("signatureId") Integer signatureId, @Param("cdIds") Collection<Integer> cdIds);

    boolean existsByReportId(Integer reportId);

    Optional<Signature> findByReportId(Integer reportId);

    List<Signature> findAllByOrderByIdDesc();
}
