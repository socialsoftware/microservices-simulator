package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum BookSagaState implements SagaState {
    DELETE_BOOK {
        @Override
        public String getStateName() {
            return "DELETE_BOOK";
        }
    },
    READ_BOOK {
        @Override
        public String getStateName() {
            return "READ_BOOK";
        }
    },
    UPDATE_BOOK {
        @Override
        public String getStateName() {
            return "UPDATE_BOOK";
        }
    }
}