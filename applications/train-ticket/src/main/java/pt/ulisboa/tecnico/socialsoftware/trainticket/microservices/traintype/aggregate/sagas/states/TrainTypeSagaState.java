package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum TrainTypeSagaState implements SagaAggregate.SagaState {
    IN_UPDATE_TRAIN_TYPE("IN_UPDATE_TRAIN_TYPE"),
    IN_DELETE_TRAIN_TYPE("IN_DELETE_TRAIN_TYPE");

    private final String stateName;

    TrainTypeSagaState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getStateName() {
        return stateName;
    }
}
