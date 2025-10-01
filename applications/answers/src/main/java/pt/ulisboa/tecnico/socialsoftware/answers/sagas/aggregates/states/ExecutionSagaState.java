package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum ExecutionSagaState implements SagaState {
CREATED_EXECUTION {
@Override
public String getStateName() {
return "CREATED_EXECUTION";
}
},
READ_EXECUTION {
@Override
public String getStateName() {
return "READ_EXECUTION";
}
}
}