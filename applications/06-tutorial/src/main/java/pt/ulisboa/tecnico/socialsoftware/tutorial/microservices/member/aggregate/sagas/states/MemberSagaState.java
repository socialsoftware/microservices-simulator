package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum MemberSagaState implements SagaState {
    CREATE_MEMBER {
        @Override
        public String getStateName() {
            return "CREATE_MEMBER";
        }
    },
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