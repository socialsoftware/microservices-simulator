package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.sagas.SagaQuiz;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@Profile("sagas")
public class SagasQuizFactory implements QuizFactory {

    @Override
    public SagaQuiz createQuiz(Integer aggregateId, String title, LocalDateTime availableDate,
                               LocalDateTime conclusionDate, LocalDateTime resultsDate, QuizType quizType,
                               QuizExecution quizExecution, Set<QuizQuestion> questions) {
        return new SagaQuiz(aggregateId, title, availableDate, conclusionDate, resultsDate, quizType, quizExecution, questions);
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
