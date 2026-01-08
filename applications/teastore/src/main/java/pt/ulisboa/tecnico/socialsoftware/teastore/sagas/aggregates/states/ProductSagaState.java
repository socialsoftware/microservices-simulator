package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum ProductSagaState implements SagaState {
    DELETE_PRODUCT {
        @Override
        public String getStateName() {
            return "DELETE_PRODUCT";
        }
    },
    READ_PRODUCT {
        @Override
        public String getStateName() {
            return "READ_PRODUCT";
        }
    },
    UPDATE_PRODUCT {
        @Override
        public String getStateName() {
            return "UPDATE_PRODUCT";
        }
    }
}