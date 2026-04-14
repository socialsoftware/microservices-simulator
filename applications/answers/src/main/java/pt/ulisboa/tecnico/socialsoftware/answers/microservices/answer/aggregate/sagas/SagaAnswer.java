package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

@Entity
public class SagaAnswer extends Answer implements SagaAggregate {
    @jakarta.persistence.Convert(converter = pt.ulisboa.tecnico.socialsoftware.answers.shared.sagaStates.SagaStateConverter.class)
    private SagaState sagaState;

    public SagaAnswer() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaAnswer(SagaAnswer other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaAnswer(Integer aggregateId, AnswerDto answerDto) {
        super(aggregateId, answerDto);
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