package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum UserSagaState implements SagaState {
    DELETE_USER {
        @Override
        public String getStateName() {
            return "DELETE_USER";
        }
    },
    READ_USER {
        @Override
        public String getStateName() {
            return "READ_USER";
        }
    },
    UPDATE_USER {
        @Override
        public String getStateName() {
            return "UPDATE_USER";
        }
    }
}