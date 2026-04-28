package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum BookingSagaState implements SagaState {
    CREATE_BOOKING {
        @Override
        public String getStateName() {
            return "CREATE_BOOKING";
        }
    },
    DELETE_BOOKING {
        @Override
        public String getStateName() {
            return "DELETE_BOOKING";
        }
    },
    READ_BOOKING {
        @Override
        public String getStateName() {
            return "READ_BOOKING";
        }
    },
    UPDATE_BOOKING {
        @Override
        public String getStateName() {
            return "UPDATE_BOOKING";
        }
    }
}