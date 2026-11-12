package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum TopicSagaState implements SagaAggregate.SagaState {
    IN_UPDATE_TOPIC("IN_UPDATE_TOPIC"),
    IN_DELETE_TOPIC("IN_DELETE_TOPIC");

    private final String stateName;

    TopicSagaState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getStateName() {
        return stateName;
    }
}
