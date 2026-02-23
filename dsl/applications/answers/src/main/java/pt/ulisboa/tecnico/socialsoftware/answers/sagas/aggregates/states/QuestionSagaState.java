package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuestionSagaState implements SagaState {
    DELETE_QUESTION {
        @Override
        public String getStateName() {
            return "DELETE_QUESTION";
        }
    },
    READ_QUESTION {
        @Override
        public String getStateName() {
            return "READ_QUESTION";
        }
    },
    UPDATE_QUESTION {
        @Override
        public String getStateName() {
            return "UPDATE_QUESTION";
        }
    }
}