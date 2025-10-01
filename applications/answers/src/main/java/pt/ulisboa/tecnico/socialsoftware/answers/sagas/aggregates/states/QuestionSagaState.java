package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuestionSagaState implements SagaState {
CREATED_QUESTION {
@Override
public String getStateName() {
return "CREATED_QUESTION";
}
},
READ_QUESTION {
@Override
public String getStateName() {
return "READ_QUESTION";
}
}
}