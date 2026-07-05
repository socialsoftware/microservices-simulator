package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum QuizAnswerSagaState implements SagaState {
    NOT_IN_SAGA {
        @Override
        public String getStateName() {
            return "NOT_IN_SAGA";
        }
    },
    READ_QUIZ_ANSWER {
        @Override
        public String getStateName() {
            return "READ_QUIZ_ANSWER";
        }
    },
    STARTED_QUIZ {
        @Override
        public String getStateName() {
            return "STARTED_QUIZ";
        }
    }
}
