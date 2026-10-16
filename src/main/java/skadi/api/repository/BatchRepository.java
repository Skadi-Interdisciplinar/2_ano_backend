package skadi.api.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import skadi.api.model.Batch;

import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    boolean existsByBatchCodeIgnoreCase(String batchCode);

    boolean existsByBatchCodeIgnoreCaseAndIdNot(String batchCode, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT batch FROM Batch batch WHERE batch.id = :batchId")
    Optional<Batch> findByIdForUpdate(@Param("batchId") Long batchId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_lote_camara_frigorifica storage
                WHERE storage.cod_lote = :batchId
                  AND storage.data_saida IS NULL
            )
            """, nativeQuery = true)
    boolean hasActiveStorage(@Param("batchId") Long batchId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM tb_lote_camara_frigorifica storage
                WHERE storage.cod_lote = :batchId
            )
            """, nativeQuery = true)
    boolean hasStorageHistory(@Param("batchId") Long batchId);
}
