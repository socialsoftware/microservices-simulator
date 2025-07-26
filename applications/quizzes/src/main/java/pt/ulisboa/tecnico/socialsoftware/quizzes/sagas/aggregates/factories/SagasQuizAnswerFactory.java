package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.SagaQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaQuizAnswerDto;

@Service
@Profile("sagas")
public class SagasQuizAnswerFactory implements QuizAnswerFactory {
    @Override
    public QuizAnswer createQuizAnswer(Integer aggregateId, AnswerCourseExecution answerCourseExecution, AnswerStudent answerStudent, AnsweredQuiz answeredQuiz) {
        return new SagaQuizAnswer(aggregateId, answerCourseExecution, answerStudent, answeredQuiz);
    }

    @Override
    public QuizAnswer createQuizAnswerFromExisting(QuizAnswer existingAnswer) {
        return new SagaQuizAnswer((SagaQuizAnswer) existingAnswer);
    }

    @Override
    public SagaQuizAnswerDto createQuizAnswerDto(QuizAnswer quizAnswer) {
        return new SagaQuizAnswerDto(quizAnswer);
    }
}
