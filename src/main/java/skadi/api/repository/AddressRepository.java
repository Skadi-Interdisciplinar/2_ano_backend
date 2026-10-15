package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skadi.api.model.Address;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    boolean existsByZipCode(String zipCode);

    boolean existsByZipCodeAndIdNot(String zipCode, Long id);

    List<Address> findAllByDistributionCenterIdOrderByIdAsc(Long distributionCenterId);
}
