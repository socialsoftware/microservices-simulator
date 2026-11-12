package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum ExecutionSagaState implements SagaAggregate.SagaState {
    IN_UPDATE_EXECUTION("IN_UPDATE_EXECUTION"),
    IN_DELETE_EXECUTION("IN_DELETE_EXECUTION"),
    IN_ENROLL_STUDENT_IN_EXECUTION("IN_ENROLL_STUDENT_IN_EXECUTION"),
    IN_DISENROLL_STUDENT("IN_DISENROLL_STUDENT");

    private final String stateName;

    ExecutionSagaState(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public String getStateName() {
        return stateName;
    }
}
