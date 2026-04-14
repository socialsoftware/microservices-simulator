package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum ContactSagaState implements SagaState {
    DELETE_CONTACT {
        @Override
        public String getStateName() {
            return "DELETE_CONTACT";
        }
    },
    READ_CONTACT {
        @Override
        public String getStateName() {
            return "READ_CONTACT";
        }
    },
    UPDATE_CONTACT {
        @Override
        public String getStateName() {
            return "UPDATE_CONTACT";
        }
    }
}