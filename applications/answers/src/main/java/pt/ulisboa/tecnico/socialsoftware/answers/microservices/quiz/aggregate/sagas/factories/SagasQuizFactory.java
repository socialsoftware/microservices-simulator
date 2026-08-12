package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.sagas.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.sagas.dtos.SagaQuizDto;

@Service
@Profile("sagas")
public class SagasQuizFactory implements QuizFactory {
    @Override
    public Quiz createQuiz(Integer aggregateId, QuizDto quizDto) {
        return new SagaQuiz(aggregateId, quizDto);
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