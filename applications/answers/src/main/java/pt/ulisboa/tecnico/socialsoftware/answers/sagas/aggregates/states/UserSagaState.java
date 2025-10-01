package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum UserSagaState implements SagaState {
CREATED_USER {
@Override
public String getStateName() {
return "CREATED_USER";
}
},
READ_USER {
@Override
public String getStateName() {
return "READ_USER";
}
}
}