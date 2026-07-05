package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum CourseExecutionSagaState implements SagaState {
    NOT_IN_SAGA {
        @Override
        public String getStateName() {
            return "NOT_IN_SAGA";
        }
    },
    IN_UPDATE_NAME {
        @Override
        public String getStateName() {
            return "IN_UPDATE_NAME";
        }
    },
    READ_COURSE {
        @Override
        public String getStateName() {
            return "READ_COURSE";
        }
    },
    READ_CREATOR {
        @Override
        public String getStateName() {
            return "READ_CREATOR";
        }
    },
    READ_STUDENT {
        @Override
        public String getStateName() {
            return "READ_STUDENT";
        }
    }
}
