package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuestionSagaState implements SagaState {
    ANSWER_QUESTION_READ_QUESTION {
        @Override
        public String getStateName() {
            return "ANSWER_QUESTION_READ_QUESTION";
        }
    },
    REMOVE_QUESTION_READ_QUESTION {
        @Override
        public String getStateName() {
            return "REMOVE_QUESTION_READ_QUESTION";
        }
    },
    UPDATE_QUESTION_READ_QUESTION {
        @Override
        public String getStateName() {
            return "UPDATE_QUESTION_READ_QUESTION";
        }
    },
    UPDATE_QUESTION_TOPICS_READ_QUESTION {
        @Override
        public String getStateName() {
            return "UPDATE_QUESTION_TOPICS_READ_QUESTION";
        }
    }
}
