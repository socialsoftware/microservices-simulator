package pt.ulisboa.tecnico.socialsoftware.helloworld.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.helloworld.sagas.aggregates.SagaTask;

@Repository
public interface TaskCustomRepositorySagas extends JpaRepository<SagaTask, Integer> {
}