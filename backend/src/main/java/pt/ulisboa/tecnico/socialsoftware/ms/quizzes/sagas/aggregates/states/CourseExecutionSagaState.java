package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum CourseExecutionSagaState implements SagaState {
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
