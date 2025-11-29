package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum ExecutionSagaState implements SagaState {
    READ_COURSE {
        @Override
        public String getStateName() {
            return "READ_COURSE";
        }
    }
}