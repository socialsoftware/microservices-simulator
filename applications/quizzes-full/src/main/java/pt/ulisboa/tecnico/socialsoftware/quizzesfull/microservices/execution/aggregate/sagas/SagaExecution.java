package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionCourse;

@Entity
public class SagaExecution extends Execution implements SagaAggregate {

    private SagaState sagaState;

    public SagaExecution() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaExecution(Integer aggregateId, String acronym, String academicTerm, ExecutionCourse executionCourse) {
        super(aggregateId, acronym, academicTerm, executionCourse);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaExecution(SagaExecution other) {
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
