package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuestionSagaState implements SagaState {
    CREATE_QUESTION {
        @Override
        public String getStateName() {
            return "CREATE_QUESTION";
        }
    },
    DELETE_QUESTION {
        @Override
        public String getStateName() {
            return "DELETE_QUESTION";
        }
    },
    READ_QUESTION {
        @Override
        public String getStateName() {
            return "READ_QUESTION";
        }
    },
    UPDATE_QUESTION {
        @Override
        public String getStateName() {
            return "UPDATE_QUESTION";
        }
    }
}