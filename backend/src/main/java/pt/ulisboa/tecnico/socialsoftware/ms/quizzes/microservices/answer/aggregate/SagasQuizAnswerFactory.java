package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuizAnswer;

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
}
