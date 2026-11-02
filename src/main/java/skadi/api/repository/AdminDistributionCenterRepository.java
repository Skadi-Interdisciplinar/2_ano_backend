package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import skadi.api.model.AdminDistributionCenter;
import skadi.api.model.AdminDistributionCenterId;
import skadi.api.model.DistributionCenter;

import java.util.List;

@Repository
public interface AdminDistributionCenterRepository
        extends JpaRepository<AdminDistributionCenter, AdminDistributionCenterId> {

    boolean existsByAdmin_IdAndDistributionCenter_Id(Long adminId, Long distributionCenterId);

    @Query("""
            SELECT link.distributionCenter
            FROM AdminDistributionCenter link
            WHERE link.admin.id = :adminId
            ORDER BY link.distributionCenter.name, link.distributionCenter.id
            """)
    List<DistributionCenter> findDistributionCentersByAdminId(@Param("adminId") Long adminId);

    @Query("""
            SELECT link.distributionCenter.id
            FROM AdminDistributionCenter link
            WHERE link.admin.id = :adminId
            """)
    List<Long> findDistributionCenterIdsByAdminId(@Param("adminId") Long adminId);

    void deleteByDistributionCenter_Id(Long distributionCenterId);
}
