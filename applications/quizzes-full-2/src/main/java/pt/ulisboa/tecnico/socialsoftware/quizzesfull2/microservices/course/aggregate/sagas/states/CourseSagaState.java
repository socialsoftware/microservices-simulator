package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum CourseSagaState implements SagaAggregate.SagaState {
    ;

    private final String stateName;

    CourseSagaState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getStateName() {
        return stateName;
    }
}
