package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum StationSagaState implements SagaAggregate.SagaState {
    IN_UPDATE_STATION("IN_UPDATE_STATION"),
    IN_DELETE_STATION("IN_DELETE_STATION");

    private final String stateName;

    StationSagaState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getStateName() {
        return stateName;
    }
}
