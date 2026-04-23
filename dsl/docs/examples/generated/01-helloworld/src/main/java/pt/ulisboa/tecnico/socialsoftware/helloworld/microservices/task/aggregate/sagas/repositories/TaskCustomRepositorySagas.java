package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.sagas.SagaTask;

@Repository
public interface TaskCustomRepositorySagas extends JpaRepository<SagaTask, Integer> {
}