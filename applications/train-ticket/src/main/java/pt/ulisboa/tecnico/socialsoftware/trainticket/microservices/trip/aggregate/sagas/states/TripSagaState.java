package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TripSagaState implements SagaState {
    DELETE_TRIP {
        @Override
        public String getStateName() {
            return "DELETE_TRIP";
        }
    },
    READ_TRIP {
        @Override
        public String getStateName() {
            return "READ_TRIP";
        }
    },
    UPDATE_TRIP {
        @Override
        public String getStateName() {
            return "UPDATE_TRIP";
        }
    }
}