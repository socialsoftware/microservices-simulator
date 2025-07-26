package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository;


@Service
@Profile("sagas")
public class QuestionCustomRepositorySagas implements QuestionCustomRepository {

    @Autowired
    private QuestionRepository questionRepository;
}
