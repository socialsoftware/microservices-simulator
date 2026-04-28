package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum CategorySagaState implements SagaState {
    CREATE_CATEGORY {
        @Override
        public String getStateName() {
            return "CREATE_CATEGORY";
        }
    },
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