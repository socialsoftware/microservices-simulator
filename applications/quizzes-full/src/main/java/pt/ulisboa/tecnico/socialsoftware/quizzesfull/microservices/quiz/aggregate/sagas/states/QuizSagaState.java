package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.sagas.states;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public enum QuizSagaState implements SagaState {
    READ_QUIZ {
        @Override
        public String getStateName() {
            return "READ_QUIZ";
        }
    },
}
