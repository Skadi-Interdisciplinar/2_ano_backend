package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skadi.api.model.SupportRequest;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {
}
