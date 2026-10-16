package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skadi.api.model.BatchStorage;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchStorageRepository extends JpaRepository<BatchStorage, Long> {

    Optional<BatchStorage> findByBatch_IdAndExitDateIsNull(Long batchId);

    Optional<BatchStorage> findByIdAndBatch_Id(Long id, Long batchId);

    List<BatchStorage> findByBatch_IdOrderByEntryDateDesc(Long batchId);
}
