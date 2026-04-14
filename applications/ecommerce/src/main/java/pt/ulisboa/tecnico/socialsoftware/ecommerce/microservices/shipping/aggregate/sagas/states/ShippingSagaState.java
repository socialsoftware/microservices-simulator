package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum ShippingSagaState implements SagaState {
    DELETE_SHIPPING {
        @Override
        public String getStateName() {
            return "DELETE_SHIPPING";
        }
    },
    READ_SHIPPING {
        @Override
        public String getStateName() {
            return "READ_SHIPPING";
        }
    },
    UPDATE_SHIPPING {
        @Override
        public String getStateName() {
            return "UPDATE_SHIPPING";
        }
    }
}