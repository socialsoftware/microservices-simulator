package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum UserSagaState implements SagaState {
    ACTIVATE_USER_READ_USER {
        @Override
        public String getStateName() {
            return "ACTIVATE_USER_READ_USER";
        }
    },
    ADD_PARTICIPANT_READ_USER {
        @Override
        public String getStateName() {
            return "ADD_PARTICIPANT_READ_USER";
        }
    },
    DELETE_USER_READ_USER {
        @Override
        public String getStateName() {
            return "DELETE_USER_READ_USER";
        }
    },
    UPDATE_STUDENT_NAME_READ_STUDENT {
        @Override
        public String getStateName() {
            return "UPDATE_STUDENT_NAME_READ_STUDENT";
        }
    },
}
