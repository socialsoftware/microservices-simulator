package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum PaymentSagaState implements SagaState {
    DELETE_PAYMENT {
        @Override
        public String getStateName() {
            return "DELETE_PAYMENT";
        }
    },
    READ_PAYMENT {
        @Override
        public String getStateName() {
            return "READ_PAYMENT";
        }
    },
    UPDATE_PAYMENT {
        @Override
        public String getStateName() {
            return "UPDATE_PAYMENT";
        }
    }
}