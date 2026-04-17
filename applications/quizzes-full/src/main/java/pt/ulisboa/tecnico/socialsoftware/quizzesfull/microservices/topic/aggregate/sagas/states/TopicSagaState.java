package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregate.SagaState;

public enum TopicSagaState implements SagaState {
    READ_TOPIC {
        @Override
        public String getStateName() {
            return "READ_TOPIC";
        }
    }
}
