package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregate.SagaState;

public enum CourseExecutionSagaState implements SagaState {
    READ_COURSE_EXECUTION {
        @Override
        public String getStateName() {
            return "READ_COURSE_EXECUTION";
        }
    }
}
