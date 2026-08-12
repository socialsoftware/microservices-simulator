package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.sagas.SagaAnswer;

@Repository
public interface AnswerCustomRepositorySagas extends JpaRepository<SagaAnswer, Integer> {
}