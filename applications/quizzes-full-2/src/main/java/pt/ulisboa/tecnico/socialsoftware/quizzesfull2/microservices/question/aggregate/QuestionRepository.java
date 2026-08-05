package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate;

import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

@Repository
public interface QuestionRepository extends AggregateRepository {
}
