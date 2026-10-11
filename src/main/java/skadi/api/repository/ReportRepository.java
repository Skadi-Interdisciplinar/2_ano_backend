package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skadi.api.model.Report;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Integer> {

    List<Report> findAllByOrderByGeneratedAtDesc();

    Optional<Report> findByIdAndGeneratedByUserId(Integer id, Integer generatedByUserId);
}
