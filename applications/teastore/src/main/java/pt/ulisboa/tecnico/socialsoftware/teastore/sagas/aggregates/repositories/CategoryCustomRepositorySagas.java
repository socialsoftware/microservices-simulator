package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.SagaCategory;

@Repository
public interface CategoryCustomRepositorySagas extends JpaRepository<SagaCategory, Integer> {
    // Saga-specific repository methods can be added here
    }