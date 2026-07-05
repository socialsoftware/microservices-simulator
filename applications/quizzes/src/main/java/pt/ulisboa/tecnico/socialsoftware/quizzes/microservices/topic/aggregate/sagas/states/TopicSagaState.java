package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum TopicSagaState implements SagaState {
    NOT_IN_SAGA {
        @Override
        public String getStateName() {
            return "NOT_IN_SAGA";
        }
    },
    READ_TOPIC {
        @Override
        public String getStateName() {
            return "READ_TOPIC";
        }
    }
}
