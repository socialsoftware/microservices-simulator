package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum RouteSagaState implements SagaState {
    DELETE_ROUTE {
        @Override
        public String getStateName() {
            return "DELETE_ROUTE";
        }
    },
    READ_ROUTE {
        @Override
        public String getStateName() {
            return "READ_ROUTE";
        }
    },
    UPDATE_ROUTE {
        @Override
        public String getStateName() {
            return "UPDATE_ROUTE";
        }
    }
}