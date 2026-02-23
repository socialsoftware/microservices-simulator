package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum CourseSagaState implements SagaState {
    DELETE_COURSE {
        @Override
        public String getStateName() {
            return "DELETE_COURSE";
        }
    },
    READ_COURSE {
        @Override
        public String getStateName() {
            return "READ_COURSE";
        }
    },
    UPDATE_COURSE {
        @Override
        public String getStateName() {
            return "UPDATE_COURSE";
        }
    }
}