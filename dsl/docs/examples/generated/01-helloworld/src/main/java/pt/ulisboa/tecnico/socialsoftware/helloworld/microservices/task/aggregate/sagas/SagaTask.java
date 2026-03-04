package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.Task;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;

@Entity
public class SagaTask extends Task implements SagaAggregate {
    private SagaState sagaState;

    public SagaTask() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTask(SagaTask other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaTask(Integer aggregateId, TaskDto taskDto) {
        super(aggregateId, taskDto);
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