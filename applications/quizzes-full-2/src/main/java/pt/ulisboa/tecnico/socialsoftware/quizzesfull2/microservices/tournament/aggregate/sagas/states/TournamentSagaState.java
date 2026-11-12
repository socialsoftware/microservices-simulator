package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum TournamentSagaState implements SagaAggregate.SagaState {
    IN_ADD_PARTICIPANT("IN_ADD_PARTICIPANT"),
    IN_UPDATE_TOURNAMENT("IN_UPDATE_TOURNAMENT"),
    IN_CANCEL_TOURNAMENT("IN_CANCEL_TOURNAMENT"),
    IN_DELETE_TOURNAMENT("IN_DELETE_TOURNAMENT");

    private final String stateName;

    TournamentSagaState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getStateName() {
        return stateName;
    }
}
