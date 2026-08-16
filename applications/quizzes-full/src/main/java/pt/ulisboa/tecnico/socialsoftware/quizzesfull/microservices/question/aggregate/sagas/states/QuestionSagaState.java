package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum QuestionSagaState implements SagaState {
    READ_QUESTION {
        @Override
        public String getStateName() {
            return "READ_QUESTION";
        }
    },
    IN_UPDATE_QUESTION {
        @Override
        public String getStateName() {
            return "IN_UPDATE_QUESTION";
        }
    },
    IN_DELETE_QUESTION {
        @Override
        public String getStateName() {
            return "IN_DELETE_QUESTION";
        }
    },
}
