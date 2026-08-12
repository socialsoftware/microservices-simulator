package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.sagas.SagaQuizAnswer;

@Service
@Profile("sagas")
public class SagasQuizAnswerFactory implements QuizAnswerFactory {

    @Override
    public SagaQuizAnswer createQuizAnswer(Integer aggregateId, Integer quizAggregateId, Long quizVersion,
                                           Integer userAggregateId, Long userVersion, String userName,
                                           String userUsername, Integer executionAggregateId, Long executionVersion) {
        return new SagaQuizAnswer(aggregateId, quizAggregateId, quizVersion,
                userAggregateId, userVersion, userName, userUsername,
                executionAggregateId, executionVersion);
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
