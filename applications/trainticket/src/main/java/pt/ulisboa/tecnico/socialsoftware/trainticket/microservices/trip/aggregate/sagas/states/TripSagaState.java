package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum TripSagaState implements SagaAggregate.SagaState {
    READ_TRIP {
        @Override
        public String getStateName() {
            return "READ_TRIP";
        }
    }
}
