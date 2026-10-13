package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum RouteSagaState implements SagaAggregate.SagaState {
    IN_UPDATE_ROUTE("IN_UPDATE_ROUTE"),
    IN_DELETE_ROUTE("IN_DELETE_ROUTE");

    private final String stateName;

    RouteSagaState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getStateName() {
        return stateName;
    }
}
