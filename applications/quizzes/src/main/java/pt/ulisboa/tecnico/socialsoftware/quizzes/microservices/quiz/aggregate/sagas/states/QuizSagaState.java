package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuizSagaState implements SagaState {
    CREATED_QUIZ {
        @Override
        public String getStateName() {
            return "CREATED_QUIZ";
        }
    },
    STARTED_TOURNAMENT_QUIZ {
        @Override
        public String getStateName() {
            return "STARTED_TOURNAMENT_QUIZ";
        }
    },
    READ_QUIZ {
        @Override
        public String getStateName() {
            return "READ_QUIZ";
        }
    }
}
