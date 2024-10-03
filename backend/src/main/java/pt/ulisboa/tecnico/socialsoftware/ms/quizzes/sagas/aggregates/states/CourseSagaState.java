package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum CourseSagaState implements SagaState {
    READ_COURSE {
        @Override
        public String getStateName() {
            return "READ_COURSE";
        }
    }
}
