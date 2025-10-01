package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaExecution;

@Repository
public interface ExecutionCustomRepositorySagas extends JpaRepository<SagaExecution, Integer> {
    // Saga-specific repository methods can be added here
    }