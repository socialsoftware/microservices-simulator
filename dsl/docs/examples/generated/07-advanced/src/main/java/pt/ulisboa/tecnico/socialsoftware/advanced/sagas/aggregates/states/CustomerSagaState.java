package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum CustomerSagaState implements SagaState {
    DELETE_CUSTOMER {
        @Override
        public String getStateName() {
            return "DELETE_CUSTOMER";
        }
    },
    READ_CUSTOMER {
        @Override
        public String getStateName() {
            return "READ_CUSTOMER";
        }
    },
    UPDATE_CUSTOMER {
        @Override
        public String getStateName() {
            return "UPDATE_CUSTOMER";
        }
    }
}