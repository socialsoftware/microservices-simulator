package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum StationSagaState implements SagaState {
    DELETE_STATION {
        @Override
        public String getStateName() {
            return "DELETE_STATION";
        }
    },
    READ_STATION {
        @Override
        public String getStateName() {
            return "READ_STATION";
        }
    },
    UPDATE_STATION {
        @Override
        public String getStateName() {
            return "UPDATE_STATION";
        }
    }
}