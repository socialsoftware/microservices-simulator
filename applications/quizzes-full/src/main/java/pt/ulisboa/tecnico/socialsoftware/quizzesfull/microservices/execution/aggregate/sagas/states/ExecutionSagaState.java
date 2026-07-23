package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum ExecutionSagaState implements SagaState {
    READ_EXECUTION {
        @Override
        public String getStateName() { return "READ_EXECUTION"; }
    },
    IN_UPDATE_EXECUTION {
        @Override
        public String getStateName() { return "IN_UPDATE_EXECUTION"; }
    },
    IN_DELETE_EXECUTION {
        @Override
        public String getStateName() { return "IN_DELETE_EXECUTION"; }
    },
    IN_ENROLL_STUDENT {
        @Override
        public String getStateName() { return "IN_ENROLL_STUDENT"; }
    },
    IN_DISENROLL_STUDENT {
        @Override
        public String getStateName() { return "IN_DISENROLL_STUDENT"; }
    },
    IN_UPDATE_STUDENT_NAME {
        @Override
        public String getStateName() { return "IN_UPDATE_STUDENT_NAME"; }
    },
    IN_ANONYMIZE_STUDENT {
        @Override
        public String getStateName() { return "IN_ANONYMIZE_STUDENT"; }
    },
}
