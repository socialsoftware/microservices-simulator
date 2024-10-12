package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.AnswerCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.AnswerStudent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.AnsweredQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaQuizAnswerDto;

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
