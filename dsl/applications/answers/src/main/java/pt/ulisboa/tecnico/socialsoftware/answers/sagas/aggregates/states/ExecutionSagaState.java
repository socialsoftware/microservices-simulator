package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum ExecutionSagaState implements SagaState {
    DELETE_EXECUTION {
        @Override
        public String getStateName() {
            return "DELETE_EXECUTION";
        }
    },
    READ_EXECUTION {
        @Override
        public String getStateName() {
            return "READ_EXECUTION";
        }
    },
    UPDATE_EXECUTION {
        @Override
        public String getStateName() {
            return "UPDATE_EXECUTION";
        }
    }
}