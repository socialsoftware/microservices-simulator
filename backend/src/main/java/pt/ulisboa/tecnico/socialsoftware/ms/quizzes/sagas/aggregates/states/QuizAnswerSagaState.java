package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuizAnswerSagaState implements SagaState {
    ANSWER_QUESTION_READ_QUIZ_ANSWER {
        @Override
        public String getStateName() {
            return "ANSWER_QUESTION_READ_QUIZ_ANSWER";
        }
    },
    CONCLUDE_QUIZ_READ_QUIZ_ANSWER {
        @Override
        public String getStateName() {
            return "CONCLUDE_QUIZ_READ_QUIZ_ANSWER";
        }
    },
    SOLVE_QUIZ_STARTED_QUIZ {
        @Override
        public String getStateName() {
            return "SOLVE_QUIZ_STARTED_QUIZ";
        }
    }
}
