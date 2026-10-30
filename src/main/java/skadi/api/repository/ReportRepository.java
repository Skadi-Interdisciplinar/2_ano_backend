package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skadi.api.model.Report;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.domain.Sort;

public interface ReportRepository extends JpaRepository<Report, Integer> {

    List<Report> findAllByDistributionCenterIdIn(Collection<Integer> cdIds, Sort sort);

    Optional<Report> findByIdAndDistributionCenterIdIn(Integer id, Collection<Integer> cdIds);

    Optional<Report> findByIdAndGeneratedByUserId(Integer id, Integer generatedByUserId);
}
