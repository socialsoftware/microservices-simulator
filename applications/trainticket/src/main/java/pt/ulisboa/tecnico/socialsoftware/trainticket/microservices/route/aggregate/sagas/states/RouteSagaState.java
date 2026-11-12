package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum RouteSagaState implements SagaAggregate.SagaState {
    READ_ROUTE {
        @Override
        public String getStateName() {
            return "READ_ROUTE";
        }
    },
    IN_UPDATE_ROUTE {
        @Override
        public String getStateName() {
            return "IN_UPDATE_ROUTE";
        }
    },
    IN_DELETE_ROUTE {
        @Override
        public String getStateName() {
            return "IN_DELETE_ROUTE";
        }
    }
}
