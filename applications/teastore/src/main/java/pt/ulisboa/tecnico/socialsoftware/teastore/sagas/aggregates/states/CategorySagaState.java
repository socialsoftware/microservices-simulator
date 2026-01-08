package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum CategorySagaState implements SagaState {
    DELETE_CATEGORY {
        @Override
        public String getStateName() {
            return "DELETE_CATEGORY";
        }
    },
    READ_CATEGORY {
        @Override
        public String getStateName() {
            return "READ_CATEGORY";
        }
    },
    UPDATE_CATEGORY {
        @Override
        public String getStateName() {
            return "UPDATE_CATEGORY";
        }
    }
}