package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum AuthorSagaState implements SagaState {
    DELETE_AUTHOR {
        @Override
        public String getStateName() {
            return "DELETE_AUTHOR";
        }
    },
    READ_AUTHOR {
        @Override
        public String getStateName() {
            return "READ_AUTHOR";
        }
    },
    UPDATE_AUTHOR {
        @Override
        public String getStateName() {
            return "UPDATE_AUTHOR";
        }
    }
}