package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuizAnswerSagaState implements SagaState {
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
