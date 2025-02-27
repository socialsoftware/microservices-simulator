package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuestionSagaState implements SagaState {
    READ_QUESTION {
        @Override
        public String getStateName() {
            return "READ_QUESTION";
        }
    }
}
