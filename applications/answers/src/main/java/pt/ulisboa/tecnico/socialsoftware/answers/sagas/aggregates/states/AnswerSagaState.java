package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum AnswerSagaState implements SagaState {
    DELETE_ANSWER {
        @Override
        public String getStateName() {
            return "DELETE_ANSWER";
        }
    },
    READ_ANSWER {
        @Override
        public String getStateName() {
            return "READ_ANSWER";
        }
    },
    UPDATE_ANSWER {
        @Override
        public String getStateName() {
            return "UPDATE_ANSWER";
        }
    }
}