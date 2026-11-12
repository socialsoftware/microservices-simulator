package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum UserSagaState implements SagaAggregate.SagaState {
    IN_ACTIVATE_USER("IN_ACTIVATE_USER"),
    IN_UPDATE_USER_NAME("IN_UPDATE_USER_NAME"),
    IN_ANONYMIZE_USER("IN_ANONYMIZE_USER"),
    IN_DELETE_USER("IN_DELETE_USER");

    private final String stateName;

    UserSagaState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getStateName() {
        return stateName;
    }
}
