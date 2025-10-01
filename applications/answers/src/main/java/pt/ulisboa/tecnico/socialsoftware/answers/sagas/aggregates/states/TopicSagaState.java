package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TopicSagaState implements SagaState {
CREATED_TOPIC {
@Override
public String getStateName() {
return "CREATED_TOPIC";
}
},
READ_TOPIC {
@Override
public String getStateName() {
return "READ_TOPIC";
}
}
}