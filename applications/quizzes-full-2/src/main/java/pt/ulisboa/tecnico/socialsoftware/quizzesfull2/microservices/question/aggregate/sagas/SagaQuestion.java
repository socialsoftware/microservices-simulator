package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.Question;

import java.time.LocalDateTime;

@Entity
public class SagaQuestion extends Question implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaQuestion() {
    }

    public SagaQuestion(Integer aggregateId, Integer courseAggregateId, String title, String content,
                        LocalDateTime creationDate) {
        super(aggregateId, courseAggregateId, title, content, creationDate);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaQuestion(SagaQuestion other) {
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
