package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaQuizDto;

@Service
@Profile("sagas")
public class SagasQuizFactory extends QuizFactory {
@Override
public Quiz createQuiz(Integer aggregateId, QuizDto quizDto) {
return new SagaQuiz(quizDto);
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