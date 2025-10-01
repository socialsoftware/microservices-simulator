package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TournamentSagaState implements SagaState {
CREATED_TOURNAMENT {
@Override
public String getStateName() {
return "CREATED_TOURNAMENT";
}
},
READ_TOURNAMENT {
@Override
public String getStateName() {
return "READ_TOURNAMENT";
}
}
}