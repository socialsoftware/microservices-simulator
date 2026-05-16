package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;

@Entity
public class SagaQuizAnswer extends QuizAnswer implements SagaAggregate {

    private SagaState sagaState;

    public SagaQuizAnswer() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuizAnswer(Integer aggregateId, Integer quizAggregateId, Long quizVersion,
                          Integer userAggregateId, Long userVersion, String userName, String userUsername,
                          Integer executionAggregateId, Long executionVersion) {
        super(aggregateId, quizAggregateId, quizVersion, userAggregateId, userVersion,
                userName, userUsername, executionAggregateId, executionVersion);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuizAnswer(SagaQuizAnswer other) {
        super(other);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }
}
