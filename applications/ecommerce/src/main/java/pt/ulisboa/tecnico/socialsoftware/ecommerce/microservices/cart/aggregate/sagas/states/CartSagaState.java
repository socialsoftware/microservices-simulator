package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum CartSagaState implements SagaState {
    DELETE_CART {
        @Override
        public String getStateName() {
            return "DELETE_CART";
        }
    },
    READ_CART {
        @Override
        public String getStateName() {
            return "READ_CART";
        }
    },
    UPDATE_CART {
        @Override
        public String getStateName() {
            return "UPDATE_CART";
        }
    }
}