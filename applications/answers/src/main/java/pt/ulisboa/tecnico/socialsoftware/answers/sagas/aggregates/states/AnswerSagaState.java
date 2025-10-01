package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum AnswerSagaState implements SagaState {
CREATED_ANSWER {
@Override
public String getStateName() {
return "CREATED_ANSWER";
}
},
READ_ANSWER {
@Override
public String getStateName() {
return "READ_ANSWER";
}
}
}