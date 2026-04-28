package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TeacherSagaState implements SagaState {
    CREATE_TEACHER {
        @Override
        public String getStateName() {
            return "CREATE_TEACHER";
        }
    },
    DELETE_TEACHER {
        @Override
        public String getStateName() {
            return "DELETE_TEACHER";
        }
    },
    READ_TEACHER {
        @Override
        public String getStateName() {
            return "READ_TEACHER";
        }
    },
    UPDATE_TEACHER {
        @Override
        public String getStateName() {
            return "UPDATE_TEACHER";
        }
    }
}