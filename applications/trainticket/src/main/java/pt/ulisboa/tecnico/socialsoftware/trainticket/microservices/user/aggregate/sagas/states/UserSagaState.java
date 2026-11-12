package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum UserSagaState implements SagaAggregate.SagaState {
    IN_UPDATE_USER {
        @Override
        public String getStateName() {
            return "IN_UPDATE_USER";
        }
    },
    IN_DELETE_USER {
        @Override
        public String getStateName() {
            return "IN_DELETE_USER";
        }
    }
}
