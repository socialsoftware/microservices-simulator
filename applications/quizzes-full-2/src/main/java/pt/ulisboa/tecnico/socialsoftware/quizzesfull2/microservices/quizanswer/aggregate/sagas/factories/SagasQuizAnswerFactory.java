package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.sagas.SagaQuizAnswer;

import java.time.LocalDateTime;

@Service
@Profile("sagas")
public class SagasQuizAnswerFactory implements QuizAnswerFactory {
    @Override
    public SagaQuizAnswer createQuizAnswer(Integer aggregateId, Integer quizAggregateId, Long quizVersion,
                                           Integer userAggregateId, String userName, Long userVersion,
                                           Integer executionAggregateId, Long executionVersion,
                                           LocalDateTime creationDate, LocalDateTime answerDate) {
        return new SagaQuizAnswer(aggregateId, quizAggregateId, quizVersion, userAggregateId, userName, userVersion,
                executionAggregateId, executionVersion, creationDate, answerDate);
    }

    @Override
    public SagaQuizAnswer createQuizAnswerCopy(QuizAnswer existing) {
        return new SagaQuizAnswer((SagaQuizAnswer) existing);
    }

    @Override
    public QuizAnswerDto createQuizAnswerDto(QuizAnswer quizAnswer) {
        return new QuizAnswerDto(quizAnswer);
    }
}
