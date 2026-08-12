package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.sagas.SagaTopic;

@Repository
public interface TopicCustomRepositorySagas extends JpaRepository<SagaTopic, Integer> {
}