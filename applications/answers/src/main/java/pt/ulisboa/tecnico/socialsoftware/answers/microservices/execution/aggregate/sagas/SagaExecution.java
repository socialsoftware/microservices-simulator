package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

@Entity
public class SagaExecution extends Execution implements SagaAggregate {
    @jakarta.persistence.Convert(converter = pt.ulisboa.tecnico.socialsoftware.answers.shared.sagaStates.SagaStateConverter.class)
    private SagaState sagaState;

    public SagaExecution() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaExecution(SagaExecution other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaExecution(Integer aggregateId, ExecutionDto executionDto) {
        super(aggregateId, executionDto);
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