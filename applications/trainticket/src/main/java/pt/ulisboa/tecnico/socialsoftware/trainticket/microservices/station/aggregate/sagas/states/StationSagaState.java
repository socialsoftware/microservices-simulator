package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum StationSagaState implements SagaAggregate.SagaState {
    READ_STATION {
        @Override
        public String getStateName() {
            return "READ_STATION";
        }
    },
    IN_UPDATE_STATION {
        @Override
        public String getStateName() {
            return "IN_UPDATE_STATION";
        }
    },
    IN_DELETE_STATION {
        @Override
        public String getStateName() {
            return "IN_DELETE_STATION";
        }
    }
}
