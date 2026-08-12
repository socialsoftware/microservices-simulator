package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum CourseSagaState implements SagaState {
    IN_UPDATE_COURSE {
        @Override
        public String getStateName() {
            return "IN_UPDATE_COURSE";
        }
    },
    IN_DELETE_COURSE {
        @Override
        public String getStateName() {
            return "IN_DELETE_COURSE";
        }
    },
    READ_COURSE {
        @Override
        public String getStateName() {
            return "READ_COURSE";
        }
    }
}
