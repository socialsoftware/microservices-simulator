package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizType;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas.SagaQuiz;

import java.time.LocalDateTime;

@Service
@Profile("sagas")
public class SagasQuizFactory implements QuizFactory {
    @Override
    public SagaQuiz createQuiz(Integer aggregateId, Integer executionAggregateId, Long executionVersion, String title,
                               LocalDateTime creationDate, LocalDateTime availableDate,
                               LocalDateTime conclusionDate, LocalDateTime resultsDate, QuizType quizType) {
        return new SagaQuiz(aggregateId, executionAggregateId, executionVersion, title, creationDate, availableDate,
                conclusionDate, resultsDate, quizType);
    }

    @Override
    public SagaQuiz createQuizCopy(Quiz existing) {
        return new SagaQuiz((SagaQuiz) existing);
    }

    @Override
    public QuizDto createQuizDto(Quiz quiz) {
        return new QuizDto(quiz);
    }
}
