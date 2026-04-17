package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregate.SagaState;

public enum UserSagaState implements SagaState {
    READ_USER {
        @Override
        public String getStateName() {
            return "READ_USER";
        }
    }
}
