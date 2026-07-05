package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum CourseSagaState implements SagaState {
    NOT_IN_SAGA {
        @Override
        public String getStateName() {
            return "NOT_IN_SAGA";
        }
    },
    READ_COURSE {
        @Override
        public String getStateName() {
            return "READ_COURSE";
        }
    }
}
