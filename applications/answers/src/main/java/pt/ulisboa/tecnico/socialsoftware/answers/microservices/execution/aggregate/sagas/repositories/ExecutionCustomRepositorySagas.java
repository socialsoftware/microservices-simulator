package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.sagas.SagaExecution;

@Repository
public interface ExecutionCustomRepositorySagas extends JpaRepository<SagaExecution, Integer> {
}