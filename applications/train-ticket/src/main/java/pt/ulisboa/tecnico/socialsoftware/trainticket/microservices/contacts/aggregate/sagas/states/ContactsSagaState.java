package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum ContactsSagaState implements SagaState {
    DELETE_CONTACTS {
        @Override
        public String getStateName() {
            return "DELETE_CONTACTS";
        }
    },
    READ_CONTACTS {
        @Override
        public String getStateName() {
            return "READ_CONTACTS";
        }
    },
    UPDATE_CONTACTS {
        @Override
        public String getStateName() {
            return "UPDATE_CONTACTS";
        }
    }
}