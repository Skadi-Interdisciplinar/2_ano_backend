package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skadi.api.model.Signature;

import java.util.List;

public interface SignatureRepository extends JpaRepository<Signature, Integer> {

    List<Signature> findByReportIdOrderByIdDesc(Integer reportId);

    boolean existsByReportId(Integer reportId);
}
