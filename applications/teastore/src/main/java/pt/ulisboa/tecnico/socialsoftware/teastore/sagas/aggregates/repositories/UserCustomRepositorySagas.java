package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.SagaUser;

@Repository
public interface UserCustomRepositorySagas extends JpaRepository<SagaUser, Integer> {
    // Saga-specific repository methods can be added here
    }