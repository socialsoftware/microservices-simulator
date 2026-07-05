package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum QuestionSagaState implements SagaState {
    NOT_IN_SAGA {
        @Override
        public String getStateName() {
            return "NOT_IN_SAGA";
        }
    },
    READ_QUESTION {
        @Override
        public String getStateName() {
            return "READ_QUESTION";
        }
    }
}
