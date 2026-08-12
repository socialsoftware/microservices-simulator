package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum QuizAnswerSagaState implements SagaState {
    READ_QUIZ_ANSWER {
        @Override
        public String getStateName() {
            return "READ_QUIZ_ANSWER";
        }
    },
    IN_ANSWER_QUESTION {
        @Override
        public String getStateName() {
            return "IN_ANSWER_QUESTION";
        }
    },
    IN_CONCLUDE_QUIZ {
        @Override
        public String getStateName() {
            return "IN_CONCLUDE_QUIZ";
        }
    },
}
