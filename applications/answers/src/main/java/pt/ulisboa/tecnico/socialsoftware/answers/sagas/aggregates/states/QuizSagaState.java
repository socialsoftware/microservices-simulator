package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuizSagaState implements SagaState {
CREATED_QUIZ {
@Override
public String getStateName() {
return "CREATED_QUIZ";
}
},
READ_QUIZ {
@Override
public String getStateName() {
return "READ_QUIZ";
}
}
}