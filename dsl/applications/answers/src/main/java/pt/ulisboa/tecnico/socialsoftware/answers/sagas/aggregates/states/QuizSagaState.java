package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuizSagaState implements SagaState {
    DELETE_QUIZ {
        @Override
        public String getStateName() {
            return "DELETE_QUIZ";
        }
    },
    READ_QUIZ {
        @Override
        public String getStateName() {
            return "READ_QUIZ";
        }
    },
    UPDATE_QUIZ {
        @Override
        public String getStateName() {
            return "UPDATE_QUIZ";
        }
    }
}