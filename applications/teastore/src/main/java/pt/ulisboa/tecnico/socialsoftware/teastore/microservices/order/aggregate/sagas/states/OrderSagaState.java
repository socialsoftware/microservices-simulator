package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum OrderSagaState implements SagaState {
    CREATE_ORDER {
        @Override
        public String getStateName() {
            return "CREATE_ORDER";
        }
    },
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