package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.SagaCart;

@Repository
public interface CartCustomRepositorySagas extends JpaRepository<SagaCart, Integer> {
    // Saga-specific repository methods can be added here
    }