package skadi.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skadi.api.model.State;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {
}
