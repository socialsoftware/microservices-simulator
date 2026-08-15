package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum QuestionSagaState implements SagaAggregate.SagaState {
    IN_UPDATE_QUESTION("IN_UPDATE_QUESTION"),
    IN_DELETE_QUESTION("IN_DELETE_QUESTION");

    private final String stateName;

    QuestionSagaState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getStateName() {
        return stateName;
    }
}
