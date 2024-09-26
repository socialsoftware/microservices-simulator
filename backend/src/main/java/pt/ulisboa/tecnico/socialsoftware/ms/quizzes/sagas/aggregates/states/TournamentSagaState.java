package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TournamentSagaState implements SagaState {
    // TODO change to more generic states
    READ_TOURNAMENT {
        @Override
        public String getStateName() {
            return "READ_TOURNAMENT";
        }
    },
    READ_UPDATED_TOPICS {
        @Override
        public String getStateName() {
            return "READ_UPDATED_TOPICS";
        }
    }
}
