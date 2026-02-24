package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum EnrollmentSagaState implements SagaState {
    DELETE_ENROLLMENT {
        @Override
        public String getStateName() {
            return "DELETE_ENROLLMENT";
        }
    },
    READ_ENROLLMENT {
        @Override
        public String getStateName() {
            return "READ_ENROLLMENT";
        }
    },
    UPDATE_ENROLLMENT {
        @Override
        public String getStateName() {
            return "UPDATE_ENROLLMENT";
        }
    }
}