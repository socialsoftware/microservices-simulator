package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TopicSagaState implements SagaState {
    DELETE_TOPIC {
        @Override
        public String getStateName() {
            return "DELETE_TOPIC";
        }
    },
    READ_TOPIC {
        @Override
        public String getStateName() {
            return "READ_TOPIC";
        }
    },
    UPDATE_TOPIC {
        @Override
        public String getStateName() {
            return "UPDATE_TOPIC";
        }
    }
}