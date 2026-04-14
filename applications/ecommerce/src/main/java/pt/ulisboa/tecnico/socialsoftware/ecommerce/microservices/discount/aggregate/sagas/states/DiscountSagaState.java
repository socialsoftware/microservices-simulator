package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum DiscountSagaState implements SagaState {
    DELETE_DISCOUNT {
        @Override
        public String getStateName() {
            return "DELETE_DISCOUNT";
        }
    },
    READ_DISCOUNT {
        @Override
        public String getStateName() {
            return "READ_DISCOUNT";
        }
    },
    UPDATE_DISCOUNT {
        @Override
        public String getStateName() {
            return "UPDATE_DISCOUNT";
        }
    }
}