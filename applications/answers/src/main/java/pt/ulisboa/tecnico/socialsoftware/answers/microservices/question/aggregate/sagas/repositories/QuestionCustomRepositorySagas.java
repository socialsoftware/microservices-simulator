package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.sagas.SagaQuestion;

@Repository
public interface QuestionCustomRepositorySagas extends JpaRepository<SagaQuestion, Integer> {
}