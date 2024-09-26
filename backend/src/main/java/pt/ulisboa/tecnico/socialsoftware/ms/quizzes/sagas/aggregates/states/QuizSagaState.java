package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuizSagaState implements SagaState {
    SOLVE_QUIZ_STARTED_TOURNAMENT_QUIZ {
        @Override
        public String getStateName() {
            return "SOLVE_QUIZ_STARTED_TOURNAMENT_QUIZ";
        }
    },
    START_QUIZ_READ_QUIZ {
        @Override
        public String getStateName() {
            return "START_QUIZ_READ_QUIZ";
        }
    }
}
