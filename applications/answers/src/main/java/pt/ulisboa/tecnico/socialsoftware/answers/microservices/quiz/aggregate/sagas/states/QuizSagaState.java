package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum QuizSagaState implements SagaState {
    CREATE_QUIZ {
        @Override
        public String getStateName() {
            return "CREATE_QUIZ";
        }
    },
    DELETE_QUIZ {
        @Override
        public String getStateName() {
            return "DELETE_QUIZ";
        }
    },
    READ_QUIZ {
        @Override
        public String getStateName() {
            return "READ_QUIZ";
        }
    },
    UPDATE_QUIZ {
        @Override
        public String getStateName() {
            return "UPDATE_QUIZ";
        }
    }
}