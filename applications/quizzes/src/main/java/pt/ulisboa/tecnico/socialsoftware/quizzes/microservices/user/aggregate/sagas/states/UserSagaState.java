package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum UserSagaState implements SagaState {
    READ_USER {
        @Override
        public String getStateName() {
            return "READ_USER";
        }
    },
    READ_STUDENT {
        @Override
        public String getStateName() {
            return "READ_STUDENT";
        }
    },
}
