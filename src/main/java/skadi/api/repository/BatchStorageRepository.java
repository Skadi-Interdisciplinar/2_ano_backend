package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skadi.api.model.BatchStorage;
import skadi.api.model.Batch;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface BatchStorageRepository extends JpaRepository<BatchStorage, Long> {

    Optional<BatchStorage> findByBatch_IdAndExitDateIsNull(Long batchId);

    Optional<BatchStorage> findByIdAndBatch_Id(Long id, Long batchId);

    List<BatchStorage> findByBatch_IdOrderByEntryDateDesc(Long batchId);

    Optional<BatchStorage> findFirstByBatch_IdOrderByEntryDateAsc(Long batchId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT DISTINCT storage.batch
            FROM BatchStorage storage
            WHERE storage.coldRoom.distributionCenter.id IN :distributionCenterIds
            ORDER BY storage.batch.batchCode
            """)
    List<Batch> findAccessibleBatches(
            @org.springframework.data.repository.query.Param("distributionCenterIds")
            Collection<Long> distributionCenterIds);
}
