package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum QuizSagaState implements SagaAggregate.SagaState {
    IN_UPDATE_QUIZ("IN_UPDATE_QUIZ");

    private final String stateName;

    QuizSagaState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getStateName() {
        return stateName;
    }
}
