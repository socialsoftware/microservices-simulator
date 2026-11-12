package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.sagas;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswer;

import java.time.LocalDateTime;

@Entity
public class SagaQuizAnswer extends QuizAnswer implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaQuizAnswer() {
    }

    public SagaQuizAnswer(Integer aggregateId, Integer quizAggregateId, Long quizVersion, Integer userAggregateId,
                          String userName, Long userVersion, Integer executionAggregateId, Long executionVersion,
                          LocalDateTime creationDate, LocalDateTime answerDate) {
        super(aggregateId, quizAggregateId, quizVersion, userAggregateId, userName, userVersion,
                executionAggregateId, executionVersion, creationDate, answerDate);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuizAnswer(SagaQuizAnswer other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}
