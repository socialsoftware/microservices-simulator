package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

public enum ContactsSagaState implements SagaAggregate.SagaState {
    READ_CONTACTS {
        @Override
        public String getStateName() {
            return "READ_CONTACTS";
        }
    },
    IN_UPDATE_CONTACTS {
        @Override
        public String getStateName() {
            return "IN_UPDATE_CONTACTS";
        }
    },
    IN_DELETE_CONTACTS {
        @Override
        public String getStateName() {
            return "IN_DELETE_CONTACTS";
        }
    }
}
