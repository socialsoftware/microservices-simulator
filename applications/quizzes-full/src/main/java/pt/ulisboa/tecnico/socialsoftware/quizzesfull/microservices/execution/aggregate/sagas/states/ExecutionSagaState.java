package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum ExecutionSagaState implements SagaState {
    READ_EXECUTION {
        @Override
        public String getStateName() {
            return "READ_EXECUTION";
        }
    },
}
