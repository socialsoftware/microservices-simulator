package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates;

import jakarta.persistence.Entity;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;
import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionDto;

@Entity
public class SagaExecution extends Execution implements SagaAggregate {
private SagaState sagaState;

public SagaExecution() {
super();
this.sagaState = GenericSagaState.NOT_IN_SAGA;
}

public SagaExecution(SagaExecution other) {
super();
this.sagaState = other.getSagaState();
}

public SagaExecution(ExecutionDto executionDto) {
super();
this.sagaState = GenericSagaState.NOT_IN_SAGA;
// TODO: Initialize from DTO properties
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