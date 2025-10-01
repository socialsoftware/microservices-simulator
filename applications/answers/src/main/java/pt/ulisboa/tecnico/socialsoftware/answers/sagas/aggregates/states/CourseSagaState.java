package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum CourseSagaState implements SagaState {
CREATED_COURSE {
@Override
public String getStateName() {
return "CREATED_COURSE";
}
},
READ_COURSE {
@Override
public String getStateName() {
return "READ_COURSE";
}
}
}