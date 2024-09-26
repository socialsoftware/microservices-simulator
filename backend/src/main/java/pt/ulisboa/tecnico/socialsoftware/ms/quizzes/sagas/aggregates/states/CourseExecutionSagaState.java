package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum CourseExecutionSagaState implements SagaState {
    ADD_STUDENT_READ_COURSE {
        @Override
        public String getStateName() {
            return "ADD_STUDENT_READ_COURSE";
        }
    },
    ANONYMIZE_STUDENT_READ_COURSE {
        @Override
        public String getStateName() {
            return "ANONYMIZE_STUDENT_READ_COURSE";
        }
    },
    CREATE_TOURNAMENT_READ_CREATOR {
        @Override
        public String getStateName() {
            return "CREATE_TOURNAMENT_READ_CREATOR";
        }
    },
    CREATE_TOURNAMENT_READ_COURSE {
        @Override
        public String getStateName() {
            return "CREATE_TOURNAMENT_READ_COURSE";
        }
    },
    REMOVE_COURSE_EXECUTION_READ_COURSE {
        @Override
        public String getStateName() {
            return "REMOVE_COURSE_EXECUTION_READ_COURSE";
        }
    },
    REMOVE_STUDENT_FROM_COURSE_EXECUTION_READ_COURSE {
        @Override
        public String getStateName() {
            return "REMOVE_STUDENT_FROM_COURSE_EXECUTION_READ_COURSE";
        }
    },
    UPDATE_STUDENT_NAME_READ_COURSE {
        @Override
        public String getStateName() {
            return "UPDATE_STUDENT_NAME_READ_COURSE";
        }
    }
}
