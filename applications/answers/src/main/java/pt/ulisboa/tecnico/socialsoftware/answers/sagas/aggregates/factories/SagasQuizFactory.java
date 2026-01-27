package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaQuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizQuestion;
import java.util.Set;

@Service
@Profile("sagas")
public class SagasQuizFactory implements QuizFactory {
    @Override
    public Quiz createQuiz(Integer aggregateId, QuizExecution execution, QuizDto quizDto, Set<QuizQuestion> questions) {
        return new SagaQuiz(aggregateId, execution, quizDto, questions);
    }

    @Override
    public Quiz createQuizFromExisting(Quiz existingQuiz) {
        return new SagaQuiz((SagaQuiz) existingQuiz);
    }

    @Override
    public QuizDto createQuizDto(Quiz quiz) {
        return new SagaQuizDto(quiz);
    }
}