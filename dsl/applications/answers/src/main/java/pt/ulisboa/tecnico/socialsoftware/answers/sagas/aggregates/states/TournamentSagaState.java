package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states;

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public enum TournamentSagaState implements SagaState {
    DELETE_TOURNAMENT {
        @Override
        public String getStateName() {
            return "DELETE_TOURNAMENT";
        }
    },
    READ_TOURNAMENT {
        @Override
        public String getStateName() {
            return "READ_TOURNAMENT";
        }
    },
    UPDATE_TOURNAMENT {
        @Override
        public String getStateName() {
            return "UPDATE_TOURNAMENT";
        }
    }
}