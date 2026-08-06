package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum OrderSagaState implements SagaState {
    DELETE_ORDER {
        @Override
        public String getStateName() {
            return "DELETE_ORDER";
        }
    },
    READ_ORDER {
        @Override
        public String getStateName() {
            return "READ_ORDER";
        }
    },
    UPDATE_ORDER {
        @Override
        public String getStateName() {
            return "UPDATE_ORDER";
        }
    }
}