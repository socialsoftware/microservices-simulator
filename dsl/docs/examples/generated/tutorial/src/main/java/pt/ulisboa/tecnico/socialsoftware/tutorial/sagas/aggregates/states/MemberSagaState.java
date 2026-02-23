package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum MemberSagaState implements SagaState {
    DELETE_MEMBER {
        @Override
        public String getStateName() {
            return "DELETE_MEMBER";
        }
    },
    READ_MEMBER {
        @Override
        public String getStateName() {
            return "READ_MEMBER";
        }
    },
    UPDATE_MEMBER {
        @Override
        public String getStateName() {
            return "UPDATE_MEMBER";
        }
    }
}