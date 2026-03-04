package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum PostSagaState implements SagaState {
    DELETE_POST {
        @Override
        public String getStateName() {
            return "DELETE_POST";
        }
    },
    READ_POST {
        @Override
        public String getStateName() {
            return "READ_POST";
        }
    },
    UPDATE_POST {
        @Override
        public String getStateName() {
            return "UPDATE_POST";
        }
    }
}