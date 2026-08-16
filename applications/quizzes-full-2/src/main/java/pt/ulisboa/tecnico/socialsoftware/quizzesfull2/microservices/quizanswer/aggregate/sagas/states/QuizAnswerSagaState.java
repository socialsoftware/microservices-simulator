package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum QuizAnswerSagaState implements SagaAggregate.SagaState {
    IN_ANSWER_QUESTION("IN_ANSWER_QUESTION"),
    IN_CONCLUDE_QUIZ("IN_CONCLUDE_QUIZ");

    private final String stateName;

    QuizAnswerSagaState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getStateName() {
        return stateName;
    }
}
